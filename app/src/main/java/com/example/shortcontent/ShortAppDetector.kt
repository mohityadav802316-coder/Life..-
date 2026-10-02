package com.example.shortcontent

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.ShortAppType
import java.security.MessageDigest

/**
 * Interface defining robust detection of short-form video screens and unique video signatures.
 * Cleanly extensible for future apps (e.g. TikTok, Moj, Snapchat Spotlight).
 */
interface ShortAppDetector {
  val targetPackages: Set<String>
  val appType: ShortAppType

  /**
   * Check if current accessibility event / window root represents a short-form video view.
   */
  fun isShortVideoScreen(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): Boolean

  /**
   * Extract a deterministic, privacy-safe signature of the current short video.
   * NEVER reads or stores user personal messages or passwords.
   */
  fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String?
}

/**
 * Common action keywords that are part of the UI chrome and should NOT be used as video signatures.
 */
private val GENERIC_UI_WORDS = setOf(
  "like", "liked", "comment", "comments", "share", "shares", "remix",
  "bookmark", "save", "saved", "send", "more", "audio", "original audio",
  "follow", "following", "subscribe", "subscribed", "dislike", "disliked",
  "back", "search", "camera", "menu", "options", "close", "mute", "unmute",
  "view profile", "pause", "play", "report", "sponsored", "reels", "shorts",
  "home", "subscriptions", "you", "profile", "explore", "inbox", "notifications",
  "watch again", "remix with this sound", "sound used in shorts"
)

/**
 * Robust Detector for Instagram Reels (com.instagram.android).
 */
class InstagramReelsDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.instagram.android")
  override val appType: ShortAppType = ShortAppType.INSTAGRAM

  private val reelIndicatorIds = setOf(
    "clips",
    "reel",
    "video_container",
    "clips_viewer",
    "reel_viewer",
    "clips_item",
    "clips_overlay_container",
    "reel_viewer_title",
    "clips_action_bar",
    "clips_author_name",
    "clips_audio_title",
    "reel_swipe_area",
    "media_view"
  )

  private val reelKeywords = setOf(
    "reels",
    "reel by",
    "original audio",
    "use audio",
    "remix",
    "share reel",
    "watch reels",
    "watch again",
    "trending audio"
  )

  override fun isShortVideoScreen(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): Boolean {
    if (rootNode == null || eventPackage?.toString() != "com.instagram.android") return false
    return try {
      val classNameStr = eventClassName?.toString() ?: ""
      if (classNameStr.contains("Clips", ignoreCase = true) ||
        classNameStr.contains("Reel", ignoreCase = true) ||
        classNameStr.contains("ClipsViewer", ignoreCase = true)
      ) {
        return true
      }

      // Check view IDs in hierarchy
      if (findAnyMatchingId(rootNode, reelIndicatorIds, depth = 0, maxDepth = 15)) {
        return true
      }

      // Check content descriptions and texts in hierarchy
      if (findAnyMatchingContentOrText(rootNode, reelKeywords, depth = 0, maxDepth = 15)) {
        return true
      }

      // Action cluster check: Like + Comment + Share with vertical aspect
      hasActionCluster(rootNode, setOf("like", "liked"), setOf("comment", "comments"), depth = 0, maxDepth = 15)
    } catch (_: Exception) {
      false
    }
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("ig_" + tokens.joinToString("::"))
      } else {
        // Fallback: robust container geometry & child structure
        val boundsToken = extractRobustContainerSignature(rootNode, "ig")
        if (boundsToken != null) {
          hashSignature("ig_bounds_" + boundsToken)
        } else {
          null
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

/**
 * Robust Detector for YouTube Shorts (com.google.android.youtube).
 */
class YouTubeShortsDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.google.android.youtube")
  override val appType: ShortAppType = ShortAppType.YOUTUBE

  private val shortsIndicatorIds = setOf(
    "shorts",
    "reel",
    "reel_recycler_view",
    "shorts_player",
    "reel_player_page_container",
    "shorts_container",
    "shorts_video_view",
    "reel_player_view",
    "reel_channel_name",
    "reel_player_title_text"
  )

  private val shortsKeywords = setOf(
    "shorts",
    "dislike this video",
    "sound used in shorts",
    "use this sound",
    "remix with this sound"
  )

  override fun isShortVideoScreen(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): Boolean {
    if (rootNode == null || eventPackage?.toString() != "com.google.android.youtube") return false
    return try {
      val classNameStr = eventClassName?.toString() ?: ""
      if (classNameStr.contains("Shorts", ignoreCase = true) ||
        classNameStr.contains("ReelPlayer", ignoreCase = true) ||
        classNameStr.contains("ShortsActivity", ignoreCase = true)
      ) {
        return true
      }

      // Check view IDs in hierarchy
      if (findAnyMatchingId(rootNode, shortsIndicatorIds, depth = 0, maxDepth = 15)) {
        return true
      }

      // Check content descriptions and texts in hierarchy
      if (findAnyMatchingContentOrText(rootNode, shortsKeywords, depth = 0, maxDepth = 15)) {
        return true
      }

      // Check for Shorts vertical action bar (Dislike button + Remix/Comments)
      hasActionCluster(rootNode, setOf("dislike", "dislike this video"), setOf("comment", "comments"), depth = 0, maxDepth = 15)
    } catch (_: Exception) {
      false
    }
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("yt_" + tokens.joinToString("::"))
      } else {
        val boundsToken = extractRobustContainerSignature(rootNode, "yt")
        if (boundsToken != null) {
          hashSignature("yt_bounds_" + boundsToken)
        } else {
          null
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

/**
 * Robust Detector for Facebook Reels (com.facebook.katana, com.facebook.lite).
 */
class FacebookReelsDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.facebook.katana", "com.facebook.lite")
  override val appType: ShortAppType = ShortAppType.FACEBOOK

  private val fbReelsIndicatorIds = setOf(
    "fb_reels",
    "watch_reel",
    "reels_viewer_root",
    "reels_video_view",
    "reel_container",
    "fb_reels_container"
  )

  private val fbReelsKeywords = setOf(
    "reels",
    "reel",
    "watch more reels",
    "original audio",
    "remix",
    "send in messenger"
  )

  override fun isShortVideoScreen(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): Boolean {
    val pkg = eventPackage?.toString() ?: ""
    if (rootNode == null || (!pkg.startsWith("com.facebook.katana") && !pkg.startsWith("com.facebook.lite"))) return false
    return try {
      val classNameStr = eventClassName?.toString() ?: ""
      if (classNameStr.contains("Reel", ignoreCase = true) || classNameStr.contains("FbReels", ignoreCase = true)) {
        return true
      }

      // Check view IDs in hierarchy
      if (findAnyMatchingId(rootNode, fbReelsIndicatorIds, depth = 0, maxDepth = 15)) {
        return true
      }

      // Check content descriptions and texts in hierarchy
      if (findAnyMatchingContentOrText(rootNode, fbReelsKeywords, depth = 0, maxDepth = 15)) {
        return true
      }

      // Check action cluster
      hasActionCluster(rootNode, setOf("like", "liked"), setOf("comment", "comments", "share"), depth = 0, maxDepth = 15)
    } catch (_: Exception) {
      false
    }
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("fb_" + tokens.joinToString("::"))
      } else {
        val boundsToken = extractRobustContainerSignature(rootNode, "fb")
        if (boundsToken != null) {
          hashSignature("fb_bounds_" + boundsToken)
        } else {
          null
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

/**
 * Registry holding all active short-content app detectors.
 */
object ShortAppRegistry {
  private val detectors = mutableListOf<ShortAppDetector>(
    InstagramReelsDetector(),
    YouTubeShortsDetector(),
    FacebookReelsDetector()
  )

  fun registerDetector(detector: ShortAppDetector) {
    if (!detectors.contains(detector)) {
      detectors.add(detector)
    }
  }

  fun getDetectorForPackage(packageName: String?): ShortAppDetector? {
    if (packageName == null) return null
    return detectors.firstOrNull { it.targetPackages.contains(packageName) }
  }

  fun getAllSupportedPackages(): Set<String> {
    return detectors.flatMap { it.targetPackages }.toSet()
  }
}

/**
 * Helper to safely find any matching view resource ID in the accessibility node hierarchy.
 */
internal fun findAnyMatchingId(
  node: AccessibilityNodeInfo?,
  targetIds: Set<String>,
  depth: Int,
  maxDepth: Int
): Boolean {
  if (node == null || depth > maxDepth) return false
  try {
    val resName = node.viewIdResourceName
    if (resName != null) {
      for (target in targetIds) {
        if (resName.contains(target, ignoreCase = true)) {
          return true
        }
      }
    }
    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      if (findAnyMatchingId(child, targetIds, depth + 1, maxDepth)) {
        return true
      }
    }
  } catch (_: Exception) {
    return false
  }
  return false
}

/**
 * Helper to check text or contentDescription keywords in the node hierarchy.
 */
internal fun findAnyMatchingContentOrText(
  node: AccessibilityNodeInfo?,
  keywords: Set<String>,
  depth: Int,
  maxDepth: Int
): Boolean {
  if (node == null || depth > maxDepth) return false
  try {
    val text = node.text?.toString()?.lowercase()
    val desc = node.contentDescription?.toString()?.lowercase()

    for (kw in keywords) {
      if ((text != null && text.contains(kw)) || (desc != null && desc.contains(kw))) {
        return true
      }
    }

    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      if (findAnyMatchingContentOrText(child, keywords, depth + 1, maxDepth)) {
        return true
      }
    }
  } catch (_: Exception) {
    return false
  }
  return false
}

/**
 * Checks for co-presence of distinct action buttons that signify a full-screen vertical player.
 */
internal fun hasActionCluster(
  node: AccessibilityNodeInfo?,
  actionGroupA: Set<String>,
  actionGroupB: Set<String>,
  depth: Int,
  maxDepth: Int
): Boolean {
  var foundA = false
  var foundB = false

  fun traverse(curr: AccessibilityNodeInfo?, d: Int) {
    if (curr == null || d > maxDepth || (foundA && foundB)) return
    try {
      val text = curr.text?.toString()?.lowercase()
      val desc = curr.contentDescription?.toString()?.lowercase()

      if (!foundA) {
        for (a in actionGroupA) {
          if ((text != null && text.contains(a)) || (desc != null && desc.contains(a))) {
            foundA = true
            break
          }
        }
      }

      if (!foundB) {
        for (b in actionGroupB) {
          if ((text != null && text.contains(b)) || (desc != null && desc.contains(b))) {
            foundB = true
            break
          }
        }
      }

      for (i in 0 until curr.childCount) {
        traverse(curr.getChild(i), d + 1)
      }
    } catch (_: Exception) {}
  }

  traverse(node, depth)
  return foundA && foundB
}

/**
 * Recursively collects public, non-sensitive text strings (e.g. channel name, video title, song)
 * strictly ignoring any passwords, input fields, or generic UI buttons.
 */
internal fun collectDistinctPublicTokens(
  node: AccessibilityNodeInfo?,
  outTokens: MutableList<String>,
  maxTokens: Int,
  depth: Int,
  maxDepth: Int
) {
  if (node == null || depth > maxDepth || outTokens.size >= maxTokens) return

  try {
    // SECURITY & PRIVACY: Absolutely skip password or editable text nodes
    if (node.isPassword || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
      return
    }

    val text = node.text?.toString()?.trim()
    val desc = node.contentDescription?.toString()?.trim()

    val candidate = when {
      !text.isNullOrBlank() && text.length in 2..120 -> text
      !desc.isNullOrBlank() && desc.length in 2..120 -> desc
      else -> null
    }

    if (candidate != null) {
      val lower = candidate.lowercase()
      val isGenericAction = GENERIC_UI_WORDS.any { lower == it || lower.startsWith("$it ") }
      if (!isGenericAction && !outTokens.contains(candidate)) {
        outTokens.add(candidate)
      }
    }

    for (i in 0 until node.childCount) {
      if (outTokens.size >= maxTokens) break
      val child = node.getChild(i) ?: continue
      collectDistinctPublicTokens(child, outTokens, maxTokens, depth + 1, maxDepth)
    }
  } catch (_: Exception) {
    // Safe graceful handling on rapid UI refresh
  }
}

/**
 * Extracts robust container signature based on geometry, child count, and layout structure.
 */
internal fun extractRobustContainerSignature(
  node: AccessibilityNodeInfo?,
  prefix: String
): String? {
  if (node == null) return null
  return try {
    val rect = Rect()
    node.getBoundsInScreen(rect)

    val childCount = node.childCount
    val hash = node.className?.toString().hashCode() xor (rect.width() * 31 + rect.height())
    "${prefix}_${rect.left}_${rect.top}_${rect.width()}x${rect.height()}_${childCount}_$hash"
  } catch (_: Exception) {
    null
  }
}

/**
 * Generate a short SHA-256 hash string for video signatures to avoid storing raw element data.
 */
internal fun hashSignature(input: String): String {
  return try {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray())
    digest.take(8).joinToString("") { "%02x".format(it) }
  } catch (_: Exception) {
    input.hashCode().toString()
  }
}
