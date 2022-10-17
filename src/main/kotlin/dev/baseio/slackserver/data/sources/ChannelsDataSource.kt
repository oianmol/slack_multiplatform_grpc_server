package dev.baseio.slackserver.data.sources

import dev.baseio.slackdata.protos.SKMessage
import dev.baseio.slackserver.data.models.SkChannel
import kotlinx.coroutines.flow.Flow

interface ChannelsDataSource {
  fun getChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel.SkGroupChannel?, SkChannel.SkGroupChannel?>>
  fun getDMChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel.SkDMChannel?, SkChannel.SkDMChannel?>>
  suspend fun savePublicChannel(toDBChannel: SkChannel.SkGroupChannel): SkChannel.SkGroupChannel?
  suspend fun saveDMChannel(skDMChannel: SkChannel.SkDMChannel): SkChannel.SkDMChannel?
  suspend fun getChannel(uuid: String, workspaceId: String): SkChannel.SkGroupChannel?
  suspend fun getDMChannel(uuid: String, workspaceId: String): SkChannel.SkDMChannel?
  suspend fun getAllChannels(workspaceId: String): List<SkChannel.SkGroupChannel>
  suspend fun getAllDMChannels(workspaceId: String): List<SkChannel.SkDMChannel>
}

