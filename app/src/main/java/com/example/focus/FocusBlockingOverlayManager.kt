package com.example.focus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.blackscreen.BlackScreenManager
import com.example.shortcontent.ShortContentAccessibilityService

/**
 * Production-ready WindowManager overlay that displays a BlockSite-style blocking screen
 * over unallowed apps when Focus Mode is active.
 */
object FocusBlockingOverlayManager {

  private const val TAG = "FocusBlockingOverlay"

  private var overlayView: FrameLayout? = null
  private var currentBlockedPackage: String? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  private var countdownTextView: TextView? = null

  private val timerTicker = object : Runnable {
    override fun run() {
      updateRemainingTime()
      if (overlayView != null) {
        mainHandler.postDelayed(this, 1000L)
      }
    }
  }

  /**
   * Displays the full-screen blocking overlay over the blocked app.
   */
  @SuppressLint("SetTextI18n")
  fun show(
    context: Context,
    blockedPackage: String,
    customTitle: String? = null,
    customNotice: String? = null,
    isShortContentLimit: Boolean = false
  ) {
    mainHandler.post {
      // If OLED Black Screen is active, keep screen dark and do not clash overlays
      if (BlackScreenManager.isOverlayActive.value) {
        Log.d(TAG, "Black screen overlay is active; skipping focus blocking card to preserve OLED state")
        return@post
      }

      // If already showing for the same blocked package, update timer and return
      if (overlayView != null && currentBlockedPackage == blockedPackage) {
        if (!isShortContentLimit) {
          updateRemainingTime()
        }
        return@post
      }

      val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

      // Remove existing overlay view first if changing package
      dismissInternal(wm)

      currentBlockedPackage = blockedPackage

      val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

      val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        layoutFlag,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
          WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.CENTER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }

      val density = context.resources.displayMetrics.density
      fun dp(value: Float): Int = (value * density + 0.5f).toInt()

      // Root Frame (Obsidian Dark Background #121214)
      val root = FrameLayout(context).apply {
        setBackgroundColor(Color.parseColor("#F5121214")) // 96% opacity dark background
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
      }

      // Center Container Card
      val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val cardBg = GradientDrawable().apply {
          setColor(Color.parseColor("#1C1C22"))
          cornerRadius = dp(24f).toFloat()
          setStroke(dp(1.2f), Color.parseColor("#B39855")) // GoldBrass accent border
        }
        background = cardBg
        setPadding(dp(24f), dp(28f), dp(24f), dp(24f))
      }

      val cardParams = FrameLayout.LayoutParams(
        dp(340f).coerceAtMost((context.resources.displayMetrics.widthPixels * 0.90f).toInt()),
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.CENTER
      }

      // App Icon or Shield
      val iconView = ImageView(context).apply {
        val pm = context.packageManager
        try {
          val appInfo = pm.getApplicationInfo(blockedPackage, 0)
          setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (_: Exception) {
          setImageResource(android.R.drawable.ic_lock_lock)
        }
        val iconSize = dp(56f)
        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
          bottomMargin = dp(16f)
        }
      }
      card.addView(iconView)

      // Title: Focus Mode Active or Custom Limit Reached
      val titleView = TextView(context).apply {
        text = customTitle ?: "🎯 Focus Mode सक्रिय"
        setTextColor(Color.parseColor(if (isShortContentLimit) "#FF5252" else "#F2C94C")) // Red for limit, Gold for focus
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = dp(6f)
        }
      }
      card.addView(titleView)

      // App Name Notice
      val pm = context.packageManager
      val appLabel = try {
        pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, 0)).toString()
      } catch (_: Exception) {
        blockedPackage
      }

      val noticeView = TextView(context).apply {
        text = customNotice ?: "“$appLabel” वर्तमान सत्र में प्रतिबंधित है। अपने मुख्य लक्ष्य पर ध्यान केंद्रित रखें।"
        setTextColor(Color.parseColor("#E0E0E0"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER
        setLineSpacing(dp(3f).toFloat(), 1.0f)
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = dp(18f)
        }
      }
      card.addView(noticeView)

      // Countdown Pill Container (hidden for daily limit reached)
      val timerPill = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        val pillBg = GradientDrawable().apply {
          setColor(Color.parseColor("#282832"))
          cornerRadius = dp(12f).toFloat()
        }
        background = pillBg
        setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = dp(24f)
        }
        if (isShortContentLimit) {
          visibility = View.GONE
        }
      }

      val timerText = TextView(context).apply {
        text = "शेष समय: गणना हो रही है..."
        setTextColor(Color.parseColor("#00E5FF")) // CyanNeon
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
      }
      countdownTextView = timerText
      timerPill.addView(timerText)
      card.addView(timerPill)

      // Button 1: Go to Home
      val homeButton = Button(context).apply {
        text = "मुख्य स्क्रीन (Go Home)"
        setTextColor(Color.parseColor("#121214"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        val btnBg = GradientDrawable().apply {
          setColor(Color.parseColor("#D4AF37")) // GoldBrass
          cornerRadius = dp(14f).toFloat()
        }
        background = btnBg
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          dp(48f)
        ).apply {
          bottomMargin = dp(10f)
        }
        setOnClickListener {
          dismiss()
          val handled = ShortContentAccessibilityService.performGlobalHome()
          if (!handled) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_HOME)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
          }
        }
      }
      card.addView(homeButton)

      // Button 2: Open Life Tracker (Unlock / Recovery)
      val appButton = Button(context).apply {
        text = "Life Tracker खोलें (अनलॉक)"
        setTextColor(Color.parseColor("#E0E0E0"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        val btnBg = GradientDrawable().apply {
          setColor(Color.TRANSPARENT)
          cornerRadius = dp(14f).toFloat()
          setStroke(dp(1f), Color.parseColor("#444455"))
        }
        background = btnBg
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          dp(44f)
        )
        setOnClickListener {
          dismiss()
          val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          }
          context.startActivity(openIntent)
        }
      }
      card.addView(appButton)

      root.addView(card, cardParams)

      try {
        wm.addView(root, params)
        overlayView = root
        mainHandler.post(timerTicker)
        Log.d(TAG, "Displayed focus blocking overlay for: $blockedPackage")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to display focus blocking overlay", e)
      }
    }
  }

  private fun updateRemainingTime() {
    val timerView = countdownTextView ?: return
    val context = timerView.context ?: return
    val remainingMillis = FocusModeManager.getRemainingSessionMillis(context)

    if (remainingMillis <= 0L) {
      timerView.text = "सत्र समाप्त हो रहा है..."
      dismiss()
      return
    }

    val totalSecs = remainingMillis / 1000L
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hrs = mins / 60
    val remMins = mins % 60

    timerView.text = if (hrs > 0) {
      String.format("शेष समय: %d घंटा %02d मिनट %02d सेकंड", hrs, remMins, secs)
    } else {
      String.format("शेष समय: %02d मिनट %02d सेकंड", mins, secs)
    }
  }

  /**
   * Dismisses and removes the blocking overlay view cleanly.
   */
  fun dismiss() {
    mainHandler.post {
      val view = overlayView ?: return@post
      val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      if (wm != null) {
        dismissInternal(wm)
      }
    }
  }

  private fun dismissInternal(wm: WindowManager) {
    mainHandler.removeCallbacks(timerTicker)
    overlayView?.let {
      try {
        wm.removeView(it)
        Log.d(TAG, "Dismissed focus blocking overlay")
      } catch (e: Exception) {
        Log.e(TAG, "Error removing focus blocking overlay", e)
      }
    }
    overlayView = null
    currentBlockedPackage = null
    countdownTextView = null
  }
}
