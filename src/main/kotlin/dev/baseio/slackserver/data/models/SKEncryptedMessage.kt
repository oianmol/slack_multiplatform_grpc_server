package dev.baseio.slackserver.data.models

data class SKEncryptedMessage(val first: ByteArray, val second: ByteArray) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as SKEncryptedMessage

    if (!first.contentEquals(other.first)) return false
    if (!second.contentEquals(other.second)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = first.contentHashCode()
    result = 31 * result + second.contentHashCode()
    return result
  }
}