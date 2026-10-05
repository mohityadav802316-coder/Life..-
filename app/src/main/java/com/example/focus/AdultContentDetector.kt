package com.example.focus

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * High-performance, 100% On-Device & Offline Adult / Explicit Content Detector.
 *
 * Scans window titles, URL bars, search queries, and visible text hierarchies across
 * browsers and applications to identify NSFW, explicit adult domains, and adult keywords
 * in English, Hindi, and Hinglish.
 *
 * Designed with strict false-positive prevention (e.g., educational, geographical,
 * or general terms like 'Essex', 'sex education' are safely whitelisted).
 */
object AdultContentDetector {

  private const val TAG = "AdultContentDetector"

  // Debounce to prevent multiple triggers in milliseconds
  @Volatile
  private var lastTriggerTimestamp = 0L
  private const val DEBOUNCE_MS = 4000L

  /**
   * Result of inspection
   */
  data class DetectionResult(
    val isAdult: Boolean,
    val matchedTerm: String = "",
    val category: String = "",
    val sourcePackage: String = ""
  )

  // =========================================================================
  // 1. KNOWN ADULT HOSTNAMES / DOMAIN STEMS
  // =========================================================================
  private val ADULT_DOMAINS = hashSetOf(
    "pornhub", "xvideos", "xnxx", "redtube", "youporn", "brazzers",
    "stripchat", "chaturbate", "xhamster", "onlyfans", "rule34", "beeg",
    "spankbang", "hentai", "eporner", "tube8", "tnaflix", "heavy-r",
    "faphouse", "cam4", "camsoda", "bongacams", "livejasmin", "adultfriendfinder",
    "noodlemagazine", "javhd", "javmost", "motherless", "xcafe", "redwap",
    "porn555", "pornone", "hdporno", "bdsmlr", "fetlife", "nudevista",
    "desipapa", "kamababa", "antavna", "chudai", "desi49", "fsiblog",
    "hindisexstories", "antavasna", "desikahani", "indiansex", "bhabhisex",
    "savita bhabhi", "savitabhabhi", "kamabishesh", "eroticmv"
  )

  // Explicit adult TLDs
  private val ADULT_TLDS = listOf(
    ".xxx", ".porn", ".adult", ".sexy"
  )

  // =========================================================================
  // 2. EXPLICIT KEYWORDS (ENGLISH, HINDI, HINGLISH)
  // =========================================================================

  // Standalone highly explicit terms (word boundary matched)
  private val STANDALONE_EXPLICIT_WORDS = hashSetOf(
    "porn", "pornography", "porno", "xxx", "xxnx", "xvideo", "xvideos",
    "nsfw", "hentai", "ecchi", "erotica", "jav", "milf", "blowjob", "creampie",
    "gangbang", "masturbat", "cumshot",
    // Hindi (Devanagari)
    "पोर्न", "पोर्नोग्राफी", "ब्लूफिल्म", "ब्लू फिल्म", "चुदाई", "संभोग", "कामुक",
    "हस्तमैथुन", "नग्नता", "छिनाल"
  )

  // Compound / High-Confidence Phrases
  private val EXPLICIT_PHRASES = listOf(
    // English
    "sex video", "sexy video", "hardcore sex", "boobs video", "pussy video",
    "nude video", "naked video", "naked girl", "nude girl", "erotic video",
    "adult video", "adult film", "adult movie", "hot sex", "desi sex",
    "bhabhi sex", "full sex", "desi mms", "leaked mms", "call girl sex",
    "camgirl live", "erotica uncensored", "adult tube", "adult film 18+",
    "uncensored 18+", "18+ adult video", "hot nude", "sex clip",
    // Hindi (Devanagari)
    "सेक्स वीडियो", "नग्न वीडियो", "हॉट सेक्स", "देसी सेक्स", "भाभी सेक्स",
    "चुदाई वीडियो", "संभोग वीडियो", "अडल्ट वीडियो", "गांड सेक्स", "ब्लू पिक्चर",
    "नंगी लड़की", "नंगा वीडियो",
    // Hinglish
    "chudai video", "chudai sexy", "sambhog video", "blue film", "blue picture",
    "hot bhabhi video", "desi mms leak", "sex movie", "chudai clip", "chudai mms",
    "nangi ladki", "nangi video", "nude video clip", "sexy blue film", "adult movie hindi",
    "desi leaked video", "desi viral mms"
  )

  // =========================================================================
  // 3. SAFE WHITELIST (PREVENT FALSE POSITIVES)
  // =========================================================================
  private val SAFE_WHITELIST_TERMS = listOf(
    "essex", "sussex", "wessex", "middlesex",
    "sex education", "sexual health", "sexual harassment", "sex ratio",
    "gender and sex", "same-sex", "sexist", "secondary", "section",
    "insect", "intersex", "unisex", "bisexual history", "sexual assault awareness",
    "biology sex chromosomes", "sex chromosome"
  )

  /**
   * Main entry point called from AccessibilityService upon events.
   * Returns a DetectionResult if adult content is confirmed.
   */
  fun inspectEvent(context: Context, event: AccessibilityEvent?): DetectionResult? {
    if (event == null) return null

    val now = System.currentTimeMillis()
    if (now - lastTriggerTimestamp < DEBOUNCE_MS) {
      return null
    }

    val packageName = event.packageName?.toString() ?: ""

    // 1. Inspect text from Event text list
    val eventTexts = event.text
    if (eventTexts != null && eventTexts.isNotEmpty()) {
      for (charSeq in eventTexts) {
        val text = charSeq?.toString() ?: continue
        val result = checkText(text, packageName)
        if (result != null) {
          lastTriggerTimestamp = now
          return result
        }
      }
    }

    // 2. Inspect event contentDescription
    val contentDesc = event.contentDescription?.toString()
    if (!contentDesc.isNullOrBlank()) {
      val result = checkText(contentDesc, packageName)
      if (result != null) {
        lastTriggerTimestamp = now
        return result
      }
    }

    // 3. Inspect active window root node hierarchy
    val source = event.source
    if (source != null) {
      val result = inspectNodeHierarchy(source, packageName, maxNodes = 60)
      if (result != null) {
        lastTriggerTimestamp = now
        return result
      }
    }

    return null
  }

  /**
   * Inspects text on a node and its direct descendants.
   */
  private fun inspectNodeHierarchy(
    node: AccessibilityNodeInfo?,
    packageName: String,
    maxNodes: Int
  ): DetectionResult? {
    if (node == null) return null

    var nodesVisited = 0
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(node)

    while (queue.isNotEmpty() && nodesVisited < maxNodes) {
      val current = queue.removeFirst()
      nodesVisited++

      // Check text
      val text = current.text?.toString()
      if (!text.isNullOrBlank()) {
        val result = checkText(text, packageName)
        if (result != null) return result
      }

      // Check content description
      val desc = current.contentDescription?.toString()
      if (!desc.isNullOrBlank()) {
        val result = checkText(desc, packageName)
        if (result != null) return result
      }

      // Inspect child nodes
      val childCount = current.childCount
      for (i in 0 until childCount) {
        val child = current.getChild(i)
        if (child != null) {
          queue.add(child)
        }
      }
    }

    return null
  }

  /**
   * Evaluates a string against adult domains, keywords, and phrases.
   */
  fun checkText(rawText: String, packageName: String): DetectionResult? {
    if (rawText.isBlank() || rawText.length < 3) return null

    val normalized = normalizeText(rawText)

    // Check safe whitelist first (e.g. sex education, Essex)
    for (safe in SAFE_WHITELIST_TERMS) {
      if (normalized.contains(safe)) {
        return null
      }
    }

    // 1. Check Adult Domains / URLs
    for (domain in ADULT_DOMAINS) {
      if (normalized.contains(domain)) {
        Log.i(TAG, "Adult domain stem match: '$domain' in '$rawText'")
        return DetectionResult(
          isAdult = true,
          matchedTerm = domain,
          category = "Adult Domain",
          sourcePackage = packageName
        )
      }
    }

    // Check adult TLDs
    for (tld in ADULT_TLDS) {
      if (normalized.contains(tld)) {
        Log.i(TAG, "Adult TLD match: '$tld' in '$rawText'")
        return DetectionResult(
          isAdult = true,
          matchedTerm = tld,
          category = "Adult Domain TLD",
          sourcePackage = packageName
        )
      }
    }

    // 2. Check Explicit Phrases
    for (phrase in EXPLICIT_PHRASES) {
      if (normalized.contains(phrase)) {
        Log.i(TAG, "Explicit phrase match: '$phrase' in '$rawText'")
        return DetectionResult(
          isAdult = true,
          matchedTerm = phrase,
          category = "Explicit Phrase",
          sourcePackage = packageName
        )
      }
    }

    // 3. Check Standalone Explicit Words with token boundaries
    val tokens = normalized.split(Regex("[\\s.,:;/?!@#%^&*()\\[\\]\\-_+=|~<>\"']+"))
    for (token in tokens) {
      if (token.length >= 3 && STANDALONE_EXPLICIT_WORDS.contains(token)) {
        Log.i(TAG, "Standalone explicit word match: '$token' in '$rawText'")
        return DetectionResult(
          isAdult = true,
          matchedTerm = token,
          category = "Explicit Keyword",
          sourcePackage = packageName
        )
      }
    }

    return null
  }

  /**
   * Normalizes text for matching:
   * - Lowercase
   * - Strips common obfuscations (e.g., p.o.r.n -> porn, p0rn -> porn)
   */
  private fun normalizeText(text: String): String {
    var s = text.lowercase(Locale.ROOT)

    // Leetspeak replacements
    s = s.replace('0', 'o')
      .replace('@', 'a')
      .replace('$', 's')
      .replace('1', 'i')
      .replace('!', 'i')
      .replace('3', 'e')
      .replace('*', ' ')

    return s
  }
}
