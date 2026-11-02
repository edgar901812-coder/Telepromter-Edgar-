package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.model.Script
import com.example.ui.components.FloatingPrompterContent

class FloatingPrompterService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var serviceLifecycleOwner: ServiceLifecycleOwner? = null

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_FONT_SIZE = "extra_font_size"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_MIRROR = "extra_mirror"

        var isRunning: Boolean = false
            private set

        fun start(context: Context, script: Script) {
            val intent = Intent(context, FloatingPrompterService::class.java).apply {
                putExtra(EXTRA_TITLE, script.title)
                putExtra(EXTRA_CONTENT, script.content)
                putExtra(EXTRA_FONT_SIZE, script.fontSize)
                putExtra(EXTRA_SPEED, script.scrollSpeed)
                putExtra(EXTRA_MIRROR, script.mirrorHorizontal)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingPrompterService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        serviceLifecycleOwner = ServiceLifecycleOwner().apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Teleprompter Flotante"
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: ""
        val fontSize = intent?.getFloatExtra(EXTRA_FONT_SIZE, 24f) ?: 24f
        val speed = intent?.getFloatExtra(EXTRA_SPEED, 55f) ?: 55f
        val mirror = intent?.getBooleanExtra(EXTRA_MIRROR, false) ?: false

        showFloatingWindow(title, content, fontSize, speed, mirror)
        return START_NOT_STICKY
    }

    private fun showFloatingWindow(
        title: String,
        content: String,
        fontSize: Float,
        speed: Float,
        mirror: Boolean
    ) {
        removeFloatingWindow()

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenWidth = displayMetrics.widthPixels

        val layoutParams = WindowManager.LayoutParams(
            (screenWidth * 0.88f).toInt().coerceAtMost((380 * density).toInt()),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ((screenWidth - width) / 2).coerceAtLeast(20)
            y = (100 * density).toInt()
        }

        val composeView = ComposeView(this).apply {
            serviceLifecycleOwner?.let { owner ->
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
            }
            setContent {
                FloatingPrompterContent(
                    title = title,
                    content = content,
                    initialFontSize = fontSize,
                    initialSpeed = speed,
                    initialMirror = mirror,
                    onClose = { stopSelf() },
                    onDragDelta = { dx, dy ->
                        layoutParams.x += dx.toInt()
                        layoutParams.y += dy.toInt()
                        windowManager?.updateViewLayout(this, layoutParams)
                    }
                )
            }
        }

        floatingView = composeView
        try {
            windowManager?.addView(composeView, layoutParams)
        } catch (_: Exception) {}
    }

    private fun removeFloatingWindow() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
            floatingView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeFloatingWindow()
        serviceLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
