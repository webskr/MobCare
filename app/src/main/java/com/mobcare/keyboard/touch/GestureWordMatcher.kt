package com.mobcare.keyboard.touch

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale

object GestureWordMatcher {

    private const val LEARNED_FILE = "learned_words.json"
    private val learnedWords = mutableMapOf<String, Int>()
    private var isLoaded = false

    // Built-in comprehensive dictionary (English + everyday Hinglish)
    private val baseWords = listOf(
        // Common Pronouns, Verbs, Connectors & Questions
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with",
        "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her",
        "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up",
        "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time",
        "no", "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", "could",
        "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
        "also", "back", "after", "use", "two", "how", "our", "work", "first", "well", "way", "even",
        "new", "want", "because", "any", "these", "give", "day", "most", "us", "is", "are", "was", "were",
        "been", "being", "had", "does", "did", "doing", "must", "might", "shall", "should", "may",

        // Conversational, Greetings & Phrases
        "hello", "hi", "hey", "dear", "thanks", "thank", "welcome", "please", "sorry", "love", "great",
        "nice", "fine", "ok", "okay", "bro", "brother", "sister", "friend", "happy", "today", "tomorrow",
        "tonight", "morning", "afternoon", "evening", "night", "yes", "cool", "super", "awesome", "perfect",
        "better", "best", "app", "phone", "mobile", "keyboard", "android", "care", "google", "chrome",
        "whatsapp", "youtube", "typing", "slide", "gesture", "fast", "smart", "quick", "clean", "code",
        "ready", "done", "start", "stop", "call", "send", "message", "reply", "help", "need", "urgent",
        "meet", "meeting", "place", "location", "home", "office", "work", "job", "money", "check", "see",
        "talk", "speak", "tell", "listen", "hear", "read", "write", "open", "close", "find", "reach",
        "leave", "wait", "soon", "later", "right", "wrong", "true", "sure", "definitely", "maybe",

        // Hinglish & Everyday Indian Messaging words
        "kya", "kaisa", "kaisi", "kaise", "kahan", "kab", "kyu", "kyun", "kaun", "kitna", "kitni", "kitne",
        "haan", "nahi", "nahin", "theek", "thik", "achha", "accha", "badiya", "sahi", "bilkul", "bhai",
        "yaar", "dost", "bolo", "batao", "suno", "dekho", "samjho", "aaj", "kal", "parso", "subah",
        "shaam", "raat", "abhi", "baad", "pehle", "jaldi", "aaram", "ruk", "ruko", "chalo", "chal",
        "aao", "jao", "karo", "karenge", "hoga", "hogi", "honge", "hua", "hui", "hue", "mera", "meri",
        "mere", "tera", "teri", "tere", "apna", "apni", "apne", "hum", "tum", "aap", "woh", "ye",
        "kuch", "sab", "bahut", "thoda", "zyada", "kam", "pata", "maloom", "yaad", "kaam", "ghar",
        "paisa", "rupaye", "bhejo", "bhej", "milte", "milenge", "shukriya", "dhanyawad", "namaste",
        "pranam", "kripya", "maaf", "pyar", "khush", "sukoon", "badhiya", "zabardast", "laajawab"
    )

    private fun ensureLoaded(context: Context) {
        if (isLoaded) return
        try {
            val file = File(context.filesDir, LEARNED_FILE)
            if (file.exists()) {
                val json = JSONObject(file.readText())
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    learnedWords[key] = json.optInt(key, 1)
                }
            }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLearned(context: Context) {
        try {
            val json = JSONObject()
            learnedWords.forEach { (word, count) ->
                json.put(word, count)
            }
            val file = File(context.filesDir, LEARNED_FILE)
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun learnWord(context: Context?, rawWord: String) {
        if (context == null) return
        val word = rawWord.lowercase(Locale.ROOT).trim().replace(Regex("[^a-zA-Z]"), "")
        if (word.length < 2) return

        ensureLoaded(context)
        val currentCount = learnedWords[word] ?: 0
        learnedWords[word] = currentCount + 1

        // Save asynchronously/directly
        saveLearned(context)
    }

    fun getSuggestions(context: Context?, prefix: String): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val query = prefix.lowercase(Locale.ROOT).trim()

        if (context != null) {
            ensureLoaded(context)
        }

        val result = mutableListOf<String>()

        // 1. Learned words matching prefix sorted by highest user usage frequency
        val learnedMatches = learnedWords.filter { (word, _) ->
            word.startsWith(query) && word != query
        }.toList().sortedByDescending { it.second }.map { it.first }

        for (w in learnedMatches) {
            if (!result.contains(w)) {
                result.add(w)
                if (result.size >= 3) return result
            }
        }

        // 2. Base dictionary words matching prefix
        val baseMatches = baseWords.filter { it.startsWith(query) && it != query }
        for (w in baseMatches) {
            if (!result.contains(w)) {
                result.add(w)
                if (result.size >= 3) return result
            }
        }

        return result
    }

    fun findBestMatch(traversedChars: List<Char>): String? {
        if (traversedChars.size < 2) return null
        val firstChar = traversedChars.first().lowercaseChar()
        val lastChar = traversedChars.last().lowercaseChar()

        val allWords = (learnedWords.keys + baseWords).distinct()

        val candidates = allWords.filter { word ->
            word.length >= 2 &&
            word.first() == firstChar &&
            word.last() == lastChar
        }

        if (candidates.isEmpty()) {
            return traversedChars.joinToString("").lowercase(Locale.ROOT)
        }

        var bestWord = candidates.first()
        var bestScore = -1

        for (candidate in candidates) {
            var score = 0
            var charIdx = 0
            for (char in candidate) {
                while (charIdx < traversedChars.size) {
                    if (traversedChars[charIdx].equals(char, ignoreCase = true)) {
                        score += 2
                        charIdx++
                        break
                    }
                    charIdx++
                }
            }
            val lenDiff = Math.abs(candidate.length - traversedChars.size)
            val finalScore = score - lenDiff
            if (finalScore > bestScore) {
                bestScore = finalScore
                bestWord = candidate
            }
        }

        return bestWord
    }
}
