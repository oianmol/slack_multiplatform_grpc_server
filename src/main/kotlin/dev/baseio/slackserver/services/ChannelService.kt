package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.*
import dev.baseio.slackserver.data.impl.ChannelMemberDataSourceImpl
import dev.baseio.slackserver.data.sources.ChannelsDataSource
import dev.baseio.slackserver.data.models.SkChannel
import dev.baseio.slackserver.data.models.SkChannelMember
import dev.baseio.slackserver.data.sources.ChannelMemberDataSource
import dev.baseio.slackserver.services.interceptors.AUTH_CONTEXT_KEY
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.coroutines.CoroutineContext

class ChannelService(
  coroutineContext: CoroutineContext = Dispatchers.IO,
  private val channelsDataSource: ChannelsDataSource,
  private val channelMemberDataSource: ChannelMemberDataSource
) :
  ChannelsServiceGrpcKt.ChannelsServiceCoroutineImplBase(coroutineContext) {

  override suspend fun channelMembers(request: SKWorkspaceChannelRequest): SKChannelMembers {
    return channelMemberDataSource.getMembers(request.workspaceId, request.channelId).run {
      SKChannelMembers.newBuilder()
        .addAllMembers(this.map { it.toGRPC() })
        .build()
    }
  }

  override suspend fun updateSKChannel(request: SKChannel): SKChannel {
    return channelsDataSource.updateChannel(request.toDBChannel())?.toGRPC()
      ?: throw StatusException(Status.NOT_FOUND)
  }

  override suspend fun saveChannel(request: SKChannel): SKChannel {
    val clientId = AUTH_CONTEXT_KEY.get()
    return channelsDataSource.insertChannel(request.toDBChannel()).toGRPC()
  }

  override fun registerChangeInChannel(request: SKChannelRequest): Flow<SKChannelChangeSnapshot> {
    return channelsDataSource.getChannelChangeStream(request.workspaceId).map { skChannel ->
      SKChannelChangeSnapshot.newBuilder()
        .apply {
          skChannel.first?.toGRPC()?.let { skMessage ->
            previous = skMessage
          }
          skChannel.second?.toGRPC()?.let { skMessage ->
            latest = skMessage
          }
        }
        .build()
    }
  }

  override suspend fun getChannels(request: SKChannelRequest): SKChannels {
    return channelsDataSource.getChannels(request.workspaceId).run {
      SKChannels.newBuilder()
        .addAllChannels(this@run.map { it.toGRPC() })
        .build()
    }
  }
}

fun SkChannelMember.toGRPC(): SKChannelMember {
  val member = this
  return sKChannelMember {
    this.uuid = member.uuid
    this.channelId = member.channelId
    this.workspaceId = member.workspaceId
    this.memberId = member.memberId
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
    .setAvatarUrl(this.avatarUrl ?: "")
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
