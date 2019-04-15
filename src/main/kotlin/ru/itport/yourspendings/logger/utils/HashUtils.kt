package utils

import java.security.MessageDigest

/**
 * Helper object to create hashes using different algos
 */
object HashUtils {
    /**
     * Supported algorithms on Android:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    fun hashString(type: String, input: String): String {
        val HEX_CHARS = "0123456789ABCDEF".toCharArray()
        try {
            val bytes = MessageDigest
                    .getInstance(type)
                    .digest(input.toByteArray())
            val result = StringBuilder(bytes.size * 2)
            for (it in bytes) {
                val i = java.lang.Byte.valueOf(it).toInt()
                result.append(HEX_CHARS[i shr 4 and 0x0f])
                result.append(HEX_CHARS[i and 0x0f])
            }
            return result.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

    }
}