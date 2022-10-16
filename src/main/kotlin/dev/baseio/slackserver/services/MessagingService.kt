package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.*
import dev.baseio.slackserver.data.models.SkChannel
import dev.baseio.slackserver.data.models.SkChannelMember
import dev.baseio.slackserver.data.sources.MessagesDataSource
import dev.baseio.slackserver.data.models.SkMessage
import dev.baseio.slackserver.data.sources.ChannelMemberDataSource
import dev.baseio.slackserver.data.sources.ChannelsDataSource
import dev.baseio.slackserver.data.sources.UsersDataSource
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class MessagingService(
  coroutineContext: CoroutineContext = Dispatchers.IO,
  private val messagesDataSource: MessagesDataSource,
  private val usersDataSource: UsersDataSource,
  private val channelsDataSource: ChannelsDataSource,
) : MessagesServiceGrpcKt.MessagesServiceCoroutineImplBase(coroutineContext) {

  override suspend fun updateMessage(request: SKMessage): SKMessage {
    // TODO validate the caller
    return messagesDataSource.updateMessage(request.toDBMessage())?.toGrpc()
      ?: throw StatusException(Status.NOT_FOUND)
  }

  override suspend fun saveMessage(request: SKMessage): SKMessage {
    try {
      val channel = channelsDataSource.getChannel(request.channelId, request.workspaceId)
      return channel?.let {
        messagesDataSource
          .saveMessage(request.toDBMessage())
          .toGrpc()
      } ?: kotlin.run {
        val channelNew = channelsDataSource.createChannel(request)
        messagesDataSource
          .saveMessage(
            request.toDBMessage()
              .copy(channelId = channelNew.uuid)
          )
          .toGrpc().copy {
            this.channel = channelNew.toGRPC()
          }
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
      return sKMessage {  }
    }

  }

  override fun registerChangeInMessage(request: SKWorkspaceChannelRequest): Flow<SKMessageChangeSnapshot> {
    return messagesDataSource.registerForChanges(request).map {
      SKMessageChangeSnapshot.newBuilder()
        .apply {
          it.first?.toGrpc()?.let { skMessage ->
            previous = skMessage
          }
          it.second?.toGrpc()?.let { skMessage ->
            latest = skMessage
          }
        }
        .build()
    }.catch {
      it.printStackTrace()
    }
  }

  override suspend fun getMessages(request: SKWorkspaceChannelRequest): SKMessages {
    val messages = messagesDataSource.getMessages(workspaceId = request.workspaceId, channelId = request.channelId)
      .map { skMessage ->
        val user = usersDataSource.getUser(skMessage.sender, skMessage.workspaceId)
        user?.let {
          skMessage.toGrpc().copy {
            senderInfo = it.toGrpc()
          }
        } ?: run {
          skMessage.toGrpc()
        }
      }
    return SKMessages.newBuilder()
      .addAllMessages(messages)
      .build()
  }
}

private fun SkMessage.toGrpc(): SKMessage {
  return SKMessage.newBuilder()
    .setUuid(this.uuid)
    .setCreatedDate(this.createdDate)
    .setModifiedDate(this.modifiedDate)
    .setWorkspaceId(this.workspaceId)
    .setChannelId(this.channelId)
    .setReceiver(this.receiver)
    .setSender(this.sender)
    .setText(this.message)
    .setIsDeleted(this.isDeleted == true)
    .build()
}

private fun SKMessage.toDBMessage(uuid: String = UUID.randomUUID().toString()): SkMessage {
  return SkMessage(
    uuid = this.uuid.takeIf { !it.isNullOrEmpty() } ?: uuid,
    workspaceId = this.workspaceId,
    channelId,
    text,
    receiver,
    sender,
    createdDate,
    modifiedDate,
    isDeleted = this.isDeleted
  )
}
