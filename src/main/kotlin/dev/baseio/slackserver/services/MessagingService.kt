package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.*
import dev.baseio.slackdata.common.SKByteArrayElement
import dev.baseio.slackserver.data.sources.MessagesDataSource
import dev.baseio.slackserver.data.models.SkMessage
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class MessagingService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    private val messagesDataSource: MessagesDataSource,
) : MessagesServiceGrpcKt.MessagesServiceCoroutineImplBase(coroutineContext) {

    override suspend fun updateMessage(request: SKMessage): SKMessage {
        // TODO validate the caller
        return messagesDataSource.updateMessage(request.toDBMessage())?.toGrpc()
            ?: throw StatusException(Status.NOT_FOUND)
    }

    override suspend fun saveMessage(request: SKMessage): SKMessage {
        return messagesDataSource
            .saveMessage(request.toDBMessage())
            .toGrpc()
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
        val messages = messagesDataSource.getMessages(
            workspaceId = request.workspaceId,
            channelId = request.channelId,
            request.paged.limit,
            request.paged.offset
        ).map { skMessage ->
            skMessage.toGrpc()
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
        .setSender(this.sender)
        .addAllText(this.message.map {
            dev.baseio.slackdata.common.sKByteArrayElement {
                this.byte = it.toInt()
            }
        })
        .setIsDeleted(this.isDeleted)
        .build()
}

private fun SKMessage.toDBMessage(uuid: String = UUID.randomUUID().toString()): SkMessage {
    return SkMessage(
        uuid = this.uuid.takeIf { !it.isNullOrEmpty() } ?: uuid,
        workspaceId = this.workspaceId,
        channelId,
        textList.map { it.byte.toByte() }.toByteArray(),
        sender,
        createdDate,
        modifiedDate,
        isDeleted = this.isDeleted
    )
}
