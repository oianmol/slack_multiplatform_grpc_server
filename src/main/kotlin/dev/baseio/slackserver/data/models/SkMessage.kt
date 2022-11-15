package dev.baseio.slackserver.data.models

import java.util.Base64

data class SkMessage(
  val uuid: String,
  val workspaceId: String,
  val channelId: String,
  val message: ByteArray,
  val sender: String,
  val createdDate: Long,
  val modifiedDate: Long,
  var isDeleted: Boolean = false
) :IDataMap{
  override fun provideMap(): HashMap<String, String> {
    return hashMapOf<String, String>().apply {
      put("uuid",uuid)
      put("workspaceId",workspaceId)
      put("channelId",channelId)
      put("sender",sender)
      put("createdDate",createdDate.toString())
      put("modifiedDate",modifiedDate.toString())
      put("isDeleted",isDeleted.toString())
      put("message", Base64.getEncoder().encodeToString(message))
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SkMessage

    if (uuid != other.uuid) return false
    if (workspaceId != other.workspaceId) return false
    if (channelId != other.channelId) return false
    if (!message.contentEquals(other.message)) return false
    if (sender != other.sender) return false
    if (createdDate != other.createdDate) return false
    if (modifiedDate != other.modifiedDate) return false
    if (isDeleted != other.isDeleted) return false

    return true
  }

  override fun hashCode(): Int {
    var result = uuid.hashCode()
    result = 31 * result + workspaceId.hashCode()
    result = 31 * result + channelId.hashCode()
    result = 31 * result + message.contentHashCode()
    result = 31 * result + sender.hashCode()
    result = 31 * result + createdDate.hashCode()
    result = 31 * result + modifiedDate.hashCode()
    result = 31 * result + isDeleted.hashCode()
    return result
  }
}