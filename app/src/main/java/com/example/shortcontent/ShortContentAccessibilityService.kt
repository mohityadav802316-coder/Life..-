package com.example.shortcontent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.focus.FocusBlockingOverlayManager
import com.example.focus.FocusModeManager
import java.lang.ref.WeakReference

/**
 * Strict-Mode Android Accessibility Service for Short Content Tracking and System-Wide Focus Blocking.
 *
 * Capabilities:
 * 1. System-Wide Focus App Interception: Detects non-allowed foreground applications and triggers
 *    BlockSite-style full overlay protection or redirects to Home.
 * 2. Privacy & Security: Only reads package identity on window state changes; never logs keystrokes,
 *    passwords, or personal chat messages.
 * 3. Short-form Video Analytics for Instagram, YouTube, and Facebook.
 */
class ShortContentAccessibilityService : AccessibilityService() {

  companion object {
    private var instanceRef: WeakReference<ShortContentAccessibilityService>? = null

    fun performGlobalHome(): Boolean {
      val service = instanceRef?.get() ?: return false
      return service.performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun isServiceRunning(): Boolean = instanceRef?.get() != null
  }

  private lateinit var trackerManager: ShortContentTrackerManager
  private var lastCheckedPackage: String? = null
  private var lastContentChangedTimestamp: Long = 0L

  override fun onCreate() {
    super.onCreate()
    instanceRef = WeakReference(this)
    trackerManager = ShortContentTrackerManager.getInstance(applicationContext)
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    instanceRef = WeakReference(this)
    Log.i("ShortContentService", "ShortContentAccessibilityService connected successfully")

    // Dynamically clear packageNames filter so service receives window changes for all apps
    try {
      val info = serviceInfo ?: AccessibilityServiceInfo()
      info.packageNames = null
      info.eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
      info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
      info.notificationTimeout = 100
      info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
      serviceInfo = info
    } catch (e: Exception) {
      Log.w("ShortContentService", "Failed to update dynamic serviceInfo", e)
    }

    try {
      trackerManager.notifyServiceConnected(true)
    } catch (_: Exception) {}
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    val packageName = event.packageName?.toString() ?: return

    // Dismiss overlay if user returns to Life Tracker or Home Launcher
    if (packageName == this.packageName || packageName.contains("launcher", ignoreCase = true)) {
      FocusBlockingOverlayManager.dismiss()
    }

    // 0. STRICT LOCK: INSTANT ON-DEVICE ADULT CONTENT DETECTION
    val adultResult = com.example.focus.AdultContentDetector.inspectEvent(this, event)
    if (adultResult != null && adultResult.isAdult) {
      Log.w("ShortContentService", "ADULT CONTENT DETECTED: matched '${adultResult.matchedTerm}' in package $packageName! Activating 15-minute Strict Lock.")

      // Immediately kick user out of adult content to Home screen
      performGlobalAction(GLOBAL_ACTION_HOME)

      // Activate unbreakable 15-minute Strict Lock Focus Mode
      FocusModeManager.activateStrictLock(
        context = this,
        durationMinutes = 15,
        reason = "वयस्क सामग्री अवरोधित: ${adultResult.matchedTerm}",
        triggerWord = adultResult.matchedTerm,
        packageName = packageName
      )

      // Immediately display unbreakable strict blocking overlay if permitted
      if (Settings.canDrawOverlays(this)) {
        FocusBlockingOverlayManager.showStrictLock(this, packageName, adultResult.matchedTerm)
      }
      return
    }

    // 1. BROAD FOCUS MODE / STRICT LOCK BLOCKING CHECK (BlockSite-Style)
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      val isStrict = FocusModeManager.isStrictLockActive(this)
      val isFocus = FocusModeManager.isFocusModeActive(this)

      if (isStrict || isFocus) {
        if (!FocusModeManager.isPackageAllowed(this, packageName)) {
          // Unallowed application opened during Focus session / Strict Lock
          // Requirement: "बाकी हर ऐप खुलते ही होम पर भेज दो।"
          performGlobalAction(GLOBAL_ACTION_HOME)

          if (Settings.canDrawOverlays(this)) {
            if (isStrict) {
              FocusBlockingOverlayManager.showStrictLock(this, packageName)
            } else {
              FocusBlockingOverlayManager.show(this, packageName)
            }
          }
          return
        } else {
          // User navigated to an allowed application, Life Tracker, Phone/Dialer, or Home Launcher
          FocusBlockingOverlayManager.dismiss()
        }
      } else {
        if (!trackerManager.isDailyLimitExceeded()) {
          FocusBlockingOverlayManager.dismiss()
        }
      }
    }

    // 2. SHORT CONTENT VIDEO TRACKING & BLOCKING (Instagram, YouTube, Facebook)
    val supportedPackages = ShortAppRegistry.getAllSupportedPackages()
    val now = System.currentTimeMillis()

    // If user navigated away from supported short-video apps
    if (!supportedPackages.contains(packageName)) {
      if (lastCheckedPackage != null && supportedPackages.contains(lastCheckedPackage)) {
        trackerManager.onExitShortVideoScreen(now)
      }
      lastCheckedPackage = packageName
      return
    }

    lastCheckedPackage = packageName

    // Debounce high-frequency content changed events (within 80ms)
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      if (now - lastContentChangedTimestamp < 80) {
        return
      }
      lastContentChangedTimestamp = now
    }

    val detector = ShortAppRegistry.getDetectorForPackage(packageName) ?: return

    try {
      // Find the best root node: rootInActiveWindow or climb from event.source
      var rootNode: AccessibilityNodeInfo? = rootInActiveWindow
      if (rootNode == null || rootNode.packageName?.toString() != packageName) {
        var src = event.source
        while (src != null && src.parent != null) {
          src = src.parent
        }
        if (src != null && src.packageName?.toString() == packageName) {
          rootNode = src
        }
      }

      if (rootNode == null) {
        trackerManager.checkInFlightVideoCommit(now)
        return
      }

      val isShortScreen = detector.isShortVideoScreen(
        rootNode = rootNode,
        eventPackage = event.packageName,
        eventClassName = event.className
      )

      if (isShortScreen) {
        // Daily Limit Blocking Check
        if (trackerManager.isDailyLimitExceeded()) {
          val limit = trackerManager.todaySummary.value?.dailyLimit ?: 20
          val title = "🚨 Reels / Shorts सीमा समाप्त"
          val notice = "दैनिक सीमा ($limit शॉर्ट्स) पूरी हो चुकी है। डिजिटल अनुशासन बनाए रखें।"

          if (Settings.canDrawOverlays(this)) {
            FocusBlockingOverlayManager.show(
              context = this,
              blockedPackage = packageName,
              customTitle = title,
              customNotice = notice,
              isShortContentLimit = true
            )
          } else {
            performGlobalAction(GLOBAL_ACTION_HOME)
          }
          trackerManager.onExitShortVideoScreen(now)
          return
        }

        // If not blocked, dismiss any old overlay and track current video
        FocusBlockingOverlayManager.dismiss()

        val signature = detector.extractVideoSignature(rootNode, event.packageName)
        if (signature != null) {
          trackerManager.onShortVideoDetected(packageName, signature, now)
        } else {
          trackerManager.checkInFlightVideoCommit(now)
        }
      } else {
        trackerManager.onExitShortVideoScreen(now)
      }
    } catch (e: Exception) {
      Log.w("ShortContentService", "Safe fallback on UI structure event: ${e.message}")
    }
  }

  override fun onInterrupt() {
    // Safe no-op
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instanceRef?.get() == this) {
      instanceRef = null
    }
    FocusBlockingOverlayManager.dismiss()
    try {
      trackerManager.notifyServiceConnected(false)
      trackerManager.onExitShortVideoScreen()
    } catch (_: Exception) {}
  }
}
