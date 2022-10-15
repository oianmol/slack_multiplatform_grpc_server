package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.*
import dev.baseio.slackserver.data.ChannelsDataSource
import dev.baseio.slackserver.data.SkChannel
import dev.baseio.slackserver.services.interceptors.AUTH_CONTEXT_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.coroutines.CoroutineContext

class ChannelService(
  coroutineContext: CoroutineContext = Dispatchers.IO,
  private val channelsDataSource: ChannelsDataSource
) :
  ChannelsServiceGrpcKt.ChannelsServiceCoroutineImplBase(coroutineContext) {

  override suspend fun saveChannel(request: SKChannel): SKChannel {
    val clientId = AUTH_CONTEXT_KEY.get()
    return channelsDataSource.insertChannel(request.toDBChannel()).toGRPC()
  }

  override fun getChannels(request: SKChannelRequest): Flow<SKChannels> {
    return super.getChannels(request)
  }
}

fun SKChannel.toDBChannel(
  workspaceId: String = UUID.randomUUID().toString(),
  channelId: String = UUID.randomUUID().toString()
): SkChannel {
  return SkChannel(
    this.uuid.takeIf { !it.isNullOrEmpty() } ?: channelId,
    this.workspaceId ?: workspaceId,
    this.name,
    createdDate,
    modifiedDate,
    isMuted, isPrivate,
    isStarred, isShareOutSide,
    isOneToOne,
    avatarUrl
  )
}

fun SkChannel.toGRPC(): SKChannel {
  return SKChannel.newBuilder()
    .setUuid(this.uuid)
    .setAvatarUrl(this.avatarUrl)
    .setName(this.name)
    .setCreatedDate(this.createdDate)
    .setIsMuted(this.isMuted)
    .setIsPrivate(this.isPrivate)
    .setIsStarred(this.isStarred)
    .setIsOneToOne(this.isOneToOne)
    .setIsShareOutSide(this.isShareOutSide)
    .setWorkspaceId(this.workspaceId)
    .setModifiedDate(this.modifiedDate)
    .build()
}
