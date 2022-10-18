package dev.baseio.slackserver.data.models

import java.util.*

sealed class SkChannel(val workspaceId: String, val channelId: String) {
  data class SkDMChannel(
    val uuid: String,
    val workId: String,
    var senderId: String,
    var receiverId: String,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val deleted: Boolean
  ) : SkChannel(workId, uuid)

  data class SkGroupChannel(
    val uuid: String,
    val workId: String,
    var name: String,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    var avatarUrl: String?,
    val deleted: Boolean
  ) : SkChannel(workId, uuid)
}


data class SkChannelMember(
  val workspaceId: String,
  val channelId: String,
  val memberId: String
) {
  var uuid: String = UUID.randomUUID().toString()
}