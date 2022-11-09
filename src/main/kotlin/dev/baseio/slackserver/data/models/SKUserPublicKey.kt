package dev.baseio.slackserver.data.models

data class SKUserPublicKey(val userId: String,
                           val algorithm: String,
                           val keyBytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SKUserPublicKey

        if (userId != other.userId) return false
        if (algorithm != other.algorithm) return false
        if (!keyBytes.contentEquals(other.keyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + keyBytes.contentHashCode()
        return result
    }
}