package com.github.kkskagvid.qrcodescanner

import android.os.Bundle

// 创建自定义扫描 Activity
class PortraitCaptureActivity : com.journeyapps.barcodescanner.CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 强制竖屏
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}