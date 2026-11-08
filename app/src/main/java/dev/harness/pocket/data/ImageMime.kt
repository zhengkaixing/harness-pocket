package dev.harness.pocket.data

/**
 * Detects the actual image container from its magic bytes.
 *
 * Android content resolvers and MediaStore can report a MIME type from the
 * file extension even when the file contents use a different image format.
 * The DSH server validates the declared type against the bytes, so callers
 * should prefer this result over the resolver-provided MIME.
 */
object ImageMime {
    fun detect(bytes: ByteArray): String? {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()
        ) {
            return "image/png"
        }
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }
        if (bytes.size >= 6) {
            val head = String(bytes, 0, 6, Charsets.US_ASCII)
            if (head == "GIF87a" || head == "GIF89a") return "image/gif"
        }
        if (bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
        ) {
            return "image/webp"
        }
        if (bytes.size >= 2 &&
            bytes[0] == 'B'.code.toByte() &&
            bytes[1] == 'M'.code.toByte()
        ) {
            return "image/bmp"
        }
        if (bytes.size >= 12 && String(bytes, 4, 4, Charsets.US_ASCII) == "ftyp") {
            return when (String(bytes, 8, 4, Charsets.US_ASCII).lowercase()) {
                "heic", "heix", "hevc", "hevx", "mif1", "msf1" -> "image/heic"
                "avif", "avis" -> "image/avif"
                else -> null
            }
        }
        return null
    }

    fun supportedImage(mime: String?): Boolean = mime in setOf(
        "image/png", "image/jpeg", "image/webp", "image/gif"
    )
}
