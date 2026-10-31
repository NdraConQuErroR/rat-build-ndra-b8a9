package com.google.appmods

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

object FlashlightController {
    var m: CameraManager? = null
    var id: String? = null
    private var blinkHandler: Handler? = null
    private var isBlinking = false

    fun set(ctx: Context, on: Boolean) {
        if (!on) {
            isBlinking = false
            blinkHandler?.removeCallbacksAndMessages(null)
        }

        if (m == null) {
            m = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            id = m?.cameraIdList?.firstOrNull()
        }
        
        try {
            m?.setTorchMode(id ?: return, on)
        } catch (e: Exception) {}
    }
    
    fun blink(ctx: Context, ms: Int) {
        if (isBlinking) return
        isBlinking = true
        blinkHandler = Handler(Looper.getMainLooper())
        var state = false
        
        val runnable = object : Runnable {
            override fun run() {
                if (!isBlinking) return
                state = !state
                set(ctx, state)
                blinkHandler?.postDelayed(this, ms.toLong())
            }
        }
        
        blinkHandler?.post(runnable)
    }
    
    fun stopBlink(ctx: Context) {
        isBlinking = false
        blinkHandler?.removeCallbacksAndMessages(null)
        set(ctx, false)
    }
}
