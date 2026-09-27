package com.aura.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
    }
    private fun checkPermission(){
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "AURA ko overlay permission do", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            startService()
        }
    }
    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) startService()
    }
    private fun startService(){
        startForegroundService(Intent(this, AURAOverlayService::class.java))
        finish()
    }
}
