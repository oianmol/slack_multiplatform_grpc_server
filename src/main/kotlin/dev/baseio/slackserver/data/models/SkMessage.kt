package dev.baseio.slackserver.data.models

import dev.baseio.slackserver.data.models.SkUser

data class SkMessage(
  val uuid: String,
  val workspaceId: String,
  val channelId: String,
  val message: String,
  val receiver: String,
  val sender: String,
  val createdDate: Long,
  val modifiedDate: Long,
  var senderInfo: SkUser? = null
)