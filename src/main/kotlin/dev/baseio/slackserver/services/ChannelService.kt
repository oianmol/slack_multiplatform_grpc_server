package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.*
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

  override suspend fun joinChannel(request: SKChannelMember): SKChannelMember {
    channelMemberDataSource.addMembers(listOf(request.toDBMember()))
    return request
  }

  override suspend fun channelMembers(request: SKWorkspaceChannelRequest): SKChannelMembers {
    return channelMemberDataSource.getMembers(request.workspaceId, request.channelId).run {
      SKChannelMembers.newBuilder()
        .addAllMembers(this.map { it.toGRPC() })
        .build()
    }
  }

  override suspend fun getAllChannels(request: SKChannelRequest): SKChannels {
    val userData = AUTH_CONTEXT_KEY.get()
    return channelsDataSource.getAllChannels(request.workspaceId, userData.userId).run {
      SKChannels.newBuilder()
        .addAllChannels(this.map { it.toGRPC() })
        .build()
    }
  }

  override suspend fun getAllDMChannels(request: SKChannelRequest): SKDMChannels {
    val userData = AUTH_CONTEXT_KEY.get()
    return channelsDataSource.getAllDMChannels(request.workspaceId, userData.userId).run {
      SKDMChannels.newBuilder()
        .addAllChannels(this.map { it.toGRPC() })
        .build()
    }
  }

  override suspend fun savePublicChannel(request: SKChannel): SKChannel {
    val authData = AUTH_CONTEXT_KEY.get()
    return channelsDataSource.savePublicChannel(request.toDBChannel(), adminId = authData.userId)?.toGRPC()
      ?: throw StatusException(Status.NOT_FOUND)
  }

  override suspend fun saveDMChannel(request: SKDMChannel): SKDMChannel {
    val authData = AUTH_CONTEXT_KEY.get()
    request.uuid?.takeIf { it.isNotEmpty() }?.let {
      return channelsDataSource.saveDMChannel(request.copy {
        createdDate = System.currentTimeMillis()
        modifiedDate = System.currentTimeMillis()
        senderId = authData.userId
      }.toDBChannel())?.toGRPC()
        ?: throw StatusException(Status.NOT_FOUND)
    } ?: run {
      if (authData.userId == request.receiverId) {
        return channelsDataSource.saveDMChannel(request.copy {
          uuid = UUID.randomUUID().toString()
          senderId = authData.userId
          receiverId = authData.userId
          createdDate = System.currentTimeMillis()
          modifiedDate = System.currentTimeMillis()
        }.toDBChannel(),)?.toGRPC()
          ?: throw StatusException(Status.NOT_FOUND)
      }
      val previousChannel = channelsDataSource.checkIfDMChannelExists(authData.userId, request.receiverId)
      previousChannel?.let {
        return it.toGRPC()
      } ?: run {
        val channel = previousChannel ?: run {
          request.copy {
            uuid = UUID.randomUUID().toString()
            senderId = authData.userId
            createdDate = System.currentTimeMillis()
            modifiedDate = System.currentTimeMillis()
          }.toDBChannel()
        }
        return channelsDataSource.saveDMChannel(channel)?.toGRPC()
          ?: throw StatusException(Status.NOT_FOUND)
      }
    }

  }

  override fun registerChangeInChannels(request: SKChannelRequest): Flow<SKChannelChangeSnapshot> {
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

  override fun registerChangeInDMChannels(request: SKChannelRequest): Flow<SKDMChannelChangeSnapshot> {
    return channelsDataSource.getDMChannelChangeStream(request.workspaceId).map { skChannel ->
      SKDMChannelChangeSnapshot.newBuilder()
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
}

private fun SKChannelMember.toDBMember(): SkChannelMember {
  return SkChannelMember( this.workspaceId, this.channelId, this.memberId).apply {
    this.uuid = this@toDBMember.uuid
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

fun SKDMChannel.toDBChannel(
): SkChannel.SkDMChannel {
  return SkChannel.SkDMChannel(
    this.uuid,
    this.workspaceId,
    this.senderId,
    this.receiverId,
    createdDate,
    modifiedDate,
    isDeleted
  )
}

fun SKChannel.toDBChannel(
  workspaceId: String = UUID.randomUUID().toString(),
  channelId: String = UUID.randomUUID().toString()
): SkChannel.SkGroupChannel {
  return SkChannel.SkGroupChannel(
    this.uuid.takeIf { !it.isNullOrEmpty() } ?: channelId,
    this.workspaceId ?: workspaceId,
    this.name,
    createdDate,
    modifiedDate,
    avatarUrl,
    isDeleted
  )
}

fun SkChannel.SkGroupChannel.toGRPC(): SKChannel {
  return SKChannel.newBuilder()
    .setUuid(this.uuid)
    .setAvatarUrl(this.avatarUrl ?: "")
    .setName(this.name)
    .setCreatedDate(this.createdDate)
    .setWorkspaceId(this.workspaceId)
    .setModifiedDate(this.modifiedDate)
    .build()
}

fun SkChannel.SkDMChannel.toGRPC(): SKDMChannel {
  return SKDMChannel.newBuilder()
    .setUuid(this.uuid)
    .setCreatedDate(this.createdDate)
    .setModifiedDate(this.modifiedDate)
    .setIsDeleted(this.deleted)
    .setReceiverId(this.receiverId)
    .setSenderId(this.senderId)
    .setWorkspaceId(this.workspaceId)
    .build()
}
