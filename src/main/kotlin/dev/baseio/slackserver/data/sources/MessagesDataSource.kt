package dev.baseio.slackserver.data.sources

import dev.baseio.slackdata.protos.SKWorkspaceChannelRequest
import kotlinx.coroutines.flow.Flow

interface MessagesDataSource {
  suspend fun saveMessage(request: SkMessage): SkMessage
  suspend fun getMessages(workspaceId: String, channelId: String): List<SkMessage>
  fun registerForChanges(request: SKWorkspaceChannelRequest): Flow<Pair<SkMessage?, SkMessage?>>
}

data class SkMessage(
  val uuid: String,
  val workspaceId: String,
  val channelId: String,
  val message: String,
  val receiver: String,
  val sender: String,
  val createdDate: Long,
  val modifiedDate: Long,
  var senderInfo: SkUser? = null
)

data class SKLastMessage(
    val channel: SkChannel,
    val message: SkMessage
)