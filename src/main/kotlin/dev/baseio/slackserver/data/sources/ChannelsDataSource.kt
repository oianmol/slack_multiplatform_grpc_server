package dev.baseio.slackserver.data.sources

import kotlinx.coroutines.flow.Flow

interface ChannelsDataSource {
  suspend fun getChannels(workspaceId:String): List<SkChannel>
  suspend fun insertChannel(channel: SkChannel): SkChannel
  fun getChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel?, SkChannel?>>
}

data class SkChannel(
  val uuid: String? = null,
  val workspaceId:String,
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