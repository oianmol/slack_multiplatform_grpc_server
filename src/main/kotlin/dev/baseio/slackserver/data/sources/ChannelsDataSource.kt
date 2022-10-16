package dev.baseio.slackserver.data.sources

import dev.baseio.slackserver.data.models.SkChannel
import kotlinx.coroutines.flow.Flow

interface ChannelsDataSource {
  suspend fun getChannels(workspaceId:String): List<SkChannel>
  suspend fun insertChannel(channel: SkChannel): SkChannel
  fun getChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel?, SkChannel?>>
  suspend fun updateChannel(toDBChannel: SkChannel): SkChannel?
  suspend fun getChannel(uuid: String, workspaceId: String): SkChannel?
}

