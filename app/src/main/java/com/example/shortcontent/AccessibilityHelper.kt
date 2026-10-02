package com.example.shortcontent

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object AccessibilityHelper {

  /**
   * Check if our ShortContentAccessibilityService is enabled in Android system settings.
   */
  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    // 1. Direct runtime check if service is alive in current process
    if (ShortContentAccessibilityService.isServiceRunning()) {
      return true
    }

    // 2. Direct query of Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES (works without query permissions)
    try {
      val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
      )
      if (!enabledServicesSetting.isNullOrEmpty() &&
        enabledServicesSetting.contains("ShortContentAccessibilityService", ignoreCase = true)
      ) {
        return true
      }
    } catch (_: Exception) {}

    // 3. Fallback to AccessibilityManager list
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: return false
    val expectedServiceName = "${context.packageName}/${ShortContentAccessibilityService::class.java.canonicalName}"
    val shortExpectedServiceName = "${context.packageName}/.shortcontent.ShortContentAccessibilityService"

    for (service in enabledServices) {
      val serviceId = service.id
      if (serviceId.equals(expectedServiceName, ignoreCase = true) ||
          serviceId.equals(shortExpectedServiceName, ignoreCase = true) ||
          serviceId.contains("ShortContentAccessibilityService", ignoreCase = true)
      ) {
        return true
      }
    }
    return false
  }

  /**
   * Launch Android Accessibility Settings screen for the user.
   */
  fun openAccessibilitySettings(context: Context) {
    try {
      val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      // Fallback to general settings if specific action fails
      try {
        val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallbackIntent)
      } catch (_: Exception) {
        // Safe no-op
      }
    }
  }
}
