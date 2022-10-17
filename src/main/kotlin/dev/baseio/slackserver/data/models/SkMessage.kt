package dev.baseio.slackserver.data.models

data class SkMessage(
  val uuid: String,
  val workspaceId: String,
  val channelId: String,
  val message: String,
  val sender: String,
  val createdDate: Long,
  val modifiedDate: Long,
  var isDeleted: Boolean = false
)