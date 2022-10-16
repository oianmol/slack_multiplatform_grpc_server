package dev.baseio.slackserver.data.models

import java.util.UUID

data class SkChannel(
  val uuid: String,
  val workspaceId: String,
  val name: String? = null,
  val createdDate: Long,
  val modifiedDate: Long,
  val isMuted: Boolean = false,
  val isPrivate: Boolean = false,
  val isStarred: Boolean = false,
  val isShareOutSide: Boolean = false,
  val isOneToOne: Boolean = false,
  val avatarUrl: String?
)

data class SkChannelMember(
  val uuid: String = UUID.randomUUID().toString(),
  val workspaceId: String,
  val channelId: String,
  val memberId: String
)