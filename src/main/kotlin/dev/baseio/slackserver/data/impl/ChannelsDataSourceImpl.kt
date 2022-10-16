package dev.baseio.slackserver.data.impl

import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.OperationType
import dev.baseio.slackdata.protos.SKMessage
import dev.baseio.slackserver.data.sources.ChannelsDataSource
import dev.baseio.slackserver.data.models.SkChannel
import dev.baseio.slackserver.data.models.SkChannelMember
import dev.baseio.slackserver.data.sources.ChannelMemberDataSource
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.match

class ChannelsDataSourceImpl(
  private val slackCloneDB: CoroutineDatabase,
  private val channelMemberDataSource: ChannelMemberDataSource
) : ChannelsDataSource {

  override suspend fun getChannel(uuid: String, workspaceId: String): SkChannel? {
    return slackCloneDB.getCollection<SkChannel>()
      .findOne(SkChannel::uuid eq uuid, SkChannel::workspaceId eq workspaceId)
  }

  override suspend fun createChannel(request: SKMessage): SkChannel {
    val existingChannel = channelMemberDataSource.isChannelExistFor(request.sender, request.receiver)
    val channelNew = existingChannel ?: run {
      //channel doesnt exist create a 1-1 channel
      val channel = SkChannel(
        request.sender + request.receiver,
        request.workspaceId,
        "",
        request.createdDate,
        request.modifiedDate,
        isMuted = false,
        isPrivate = true,
        isStarred = false,
        isShareOutSide = false,
        isOneToOne = true,
        avatarUrl = null
      )
      channelMemberDataSource.addMembers(
        listOf(
          SkChannelMember(channelId = channel.uuid, memberId = request.sender, workspaceId = request.workspaceId),
          SkChannelMember(channelId = channel.uuid, memberId = request.receiver, workspaceId = request.workspaceId)
        )
      )
      insertChannel(
        channel
      )
    }
    return channelNew
  }

  override suspend fun updateChannel(request: SkChannel): SkChannel? {
    slackCloneDB.getCollection<SkChannel>()
      .updateOne(SkChannel::uuid eq request.uuid, request)
    return getChannel(request.uuid, request.workspaceId)
  }


  override fun getChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel?, SkChannel?>> {
    val collection = slackCloneDB.getCollection<SkChannel>()

    val pipeline: List<Bson> = listOf(
      match(
        Document.parse("{'fullDocument.workspaceId': '$workspaceId'}"),
        Filters.`in`("operationType", OperationType.values().map { it.value }.toList())
      )
    )

    return collection
      .watch<SkChannel>(pipeline).toFlow().mapNotNull {
        Pair(it.fullDocumentBeforeChange, it.fullDocument)
      }
  }

  override suspend fun getChannels(workspaceId: String): List<SkChannel> {
    return slackCloneDB.getCollection<SkChannel>()
      .find(SkChannel::workspaceId eq workspaceId)
      .toList()

  }

  override suspend fun insertChannel(channel: SkChannel): SkChannel {
    return slackCloneDB.getCollection<SkChannel>().run {
      insertOne(channel)
      findOne(SkChannel::uuid eq channel.uuid) ?: throw StatusException(Status.CANCELLED)
    }
  }
}