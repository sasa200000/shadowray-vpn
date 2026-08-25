package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.widget.Toast
import com.example.model.ProxyConfig
import com.example.parser.ConfigParser

/**
 * Access-key support for ShadowRay VPN (کلید ورود).
 *
 * A key looks like: SR-XXXXX.XXXXX.XXXXX  (Base32 A-Z/2-7, dots as separators)
 *
 * The key encodes a hidden payload — a standard config URI, a list of URIs,
 * or a whole base64 subscription body — using XOR(0x5A) + Base32.
 * Keys are case-insensitive; spaces/dots/dashes typed by the user are ignored.
 * Companion PC-side generator: tools/gen_key.py in the repo.
 */
object AccessKey {

    private const val KEY_PREFIX = "SR-"
    private const val MAGIC = 0x5A

    /** Generate an access key that decodes to [payload]. */
    fun encodeKey(payload: String): String {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val xored = ByteArray(bytes.size) { (bytes[it].toInt() xor MAGIC).toByte() }
        val b32 = Base32.encode(xored)
        return KEY_PREFIX + b32.chunked(5).joinToString(".")
    }

    /** Decode a user-entered key back into the payload. Null if invalid. */
    fun decodeKey(keyRaw: String): String? {
        var body = keyRaw.trim().uppercase().replace(" ", "")
        if (!body.startsWith(KEY_PREFIX)) return null
        body = body.removePrefix(KEY_PREFIX).replace(".", "").replace("-", "")
        if (body.length < 8) return null
        if (body.any { it == '0' || it == '1' || it == '8' || it == '9' }) return null
        return try {
            val xored = Base32.decode(body)
            val bytes = ByteArray(xored.size) { (xored[it].toInt() xor MAGIC).toByte() }
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolve pasted text: raw config URI(s), subscription URL or SR- key.
     * Returns parsed configs (empty if nothing valid).
     */
    fun resolve(text: String): List<ProxyConfig> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith(KEY_PREFIX, ignoreCase = true)) {
            return ConfigParser.extractAllConfigs(trimmed)
        }
        val payload = decodeKey(trimmed) ?: return emptyList()
        return ConfigParser.extractAllConfigs(payload)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
}

/** Minimal RFC-4648 Base32 (A-Z, 2-7) helper. */
private object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(data: ByteArray): String {
        var bits = 0
        var value = 0
        val out = StringBuilder()
        for (b in data) {
            value = (value shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(value ushr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(value shl (5 - bits)) and 31])
        return out.toString()
    }

    fun decode(encoded: String): ByteArray {
        var bits = 0
        var value = 0
        val out = ArrayList<Byte>(encoded.length * 5 / 8)
        for (c in encoded.uppercase()) {
            val idx = ALPHABET.indexOf(c)
            require(idx >= 0) { "invalid base32 char: $c" }
            value = (value shl 5) or idx
            bits += 5
            if (bits >= 8) {
                out.add(((value ushr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
