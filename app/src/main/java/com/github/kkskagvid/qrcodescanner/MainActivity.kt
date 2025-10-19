package com.github.kkskagvid.qrcodescanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private val scanContract = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            // 扫描被取消，等待用户操作
            showScanCancelledDialog()
        } else {
            // 扫描成功，弹出结果对话框
            showResultDialog(result.contents)
        }
    }

    private lateinit var statusText: TextView
    private lateinit var rescanButton: Button
    private var isWaitingForRescan = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置竖屏
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 初始化视图
        initViews()

        // 延迟启动扫描，确保界面已经加载完成
        findViewById<android.view.View>(android.R.id.content).postDelayed({
            startScan()
        }, 500)
    }

    private fun initViews() {
        statusText = findViewById(R.id.status_text)
        rescanButton = findViewById(R.id.rescan_button)

        // 设置重新扫描按钮的点击事件
        rescanButton.setOnClickListener {
            startScan()
        }

        // 初始隐藏重新扫描按钮
        rescanButton.visibility = Button.GONE

        initSettingsButton()
    }

    private fun startScan() {
        // 重置状态
        isWaitingForRescan = false
        updateStatusText("正在启动扫描器...")
        hideRescanButton()

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            // 请求相机权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被授予，启动扫描
                    launchScanner()
                } else {
                    // 权限被拒绝
                    updateStatusText("需要相机权限才能扫描")
                    Toast.makeText(
                        this,
                        "需要相机权限才能扫描二维码",
                        Toast.LENGTH_LONG
                    ).show()
                    // 显示重新扫描按钮
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    private fun launchScanner() {
        try {
            updateStatusText("请将二维码对准扫描框")

            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("将二维码对准扫描框") // 设置提示文字
                setCameraId(0) // 使用后置摄像头
                setBeepEnabled(false) // 关闭提示音
                setBarcodeImageEnabled(true)
                setOrientationLocked(true) // 锁定竖屏方向
                // 指定使用自定义的竖屏 Activity
                setCaptureActivity(PortraitCaptureActivity::class.java)
            }
            scanContract.launch(options)
        } catch (e: Exception) {
            updateStatusText("扫描启动失败")
            Toast.makeText(this, "启动扫描器失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            // 发生异常时，显示重新扫描选项
            showErrorDialog()
        }
    }

    private fun showResultDialog(content: String) {
        AlertDialog.Builder(this)
            .setTitle("扫描成功")
            .setMessage(content)
            .setPositiveButton("复制") { _, _ ->
                // 复制到剪贴板
                copyToClipboard(content)
                // 复制后显示继续扫描选项
                showContinueScanDialog()
            }
            .setNegativeButton("继续扫描") { _, _ ->
                // 直接重新开始扫描
                startScan()
            }
            .setCancelable(false) // 防止用户点击外部取消
            .show()
    }

    private fun showScanCancelledDialog() {
        AlertDialog.Builder(this)
            .setTitle("扫描已取消")
            .setMessage("扫描过程被中断")
            .setPositiveButton("重新扫描") { _, _ ->
                startScan()
            }
            .setNegativeButton("稍后扫描") { _, _ ->
                // 设置等待重新扫描状态
                setWaitingForRescan()
            }
            .setCancelable(false)
            .show()
    }

    private fun showContinueScanDialog() {
        AlertDialog.Builder(this)
            .setTitle("操作完成")
            .setMessage("内容已复制到剪贴板")
            .setPositiveButton("继续扫描") { _, _ ->
                startScan()
            }
            .setNegativeButton("稍后扫描") { _, _ ->
                // 设置等待重新扫描状态
                setWaitingForRescan()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("需要相机权限才能扫描二维码。请在设置中授予权限，然后重新启动应用。")
            .setPositiveButton("重新请求权限") { _, _ ->
                startScan()
            }
            .setNegativeButton("稍后扫描") { _, _ ->
                // 设置等待重新扫描状态
                setWaitingForRescan()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("扫描失败")
            .setMessage("启动扫描器时发生错误")
            .setPositiveButton("重试") { _, _ ->
                startScan()
            }
            .setNegativeButton("稍后扫描") { _, _ ->
                // 设置等待重新扫描状态
                setWaitingForRescan()
            }
            .setCancelable(false)
            .show()
    }

    private fun setWaitingForRescan() {
        isWaitingForRescan = true
        updateStatusText("点击下方按钮重新开始扫描")
        showRescanButton()
    }

    private fun updateStatusText(text: String) {
        statusText.text = text
    }

    private fun showRescanButton() {
        rescanButton.visibility = Button.VISIBLE
    }

    private fun hideRescanButton() {
        rescanButton.visibility = Button.GONE
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun initSettingsButton() {
        val settingsButton: ImageButton = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage("二维码扫描器 v1.0\n基于 ZXing 开发\n竖屏模式")
            .setPositiveButton("确定", null)
            .show()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}