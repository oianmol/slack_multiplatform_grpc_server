package dev.baseio.slackserver.data.impl

import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.OperationType
import dev.baseio.slackserver.data.sources.ChannelsDataSource
import dev.baseio.slackserver.data.models.SkChannel
import dev.baseio.slackserver.data.models.SkChannelMember
import dev.baseio.slackserver.data.sources.ChannelMemberDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.`in`
import org.litote.kmongo.match

class ChannelsDataSourceImpl(
  private val slackCloneDB: CoroutineDatabase,
  private val channelMemberDataSource: ChannelMemberDataSource
) : ChannelsDataSource {

  override suspend fun getAllChannels(workspaceId: String, userId: String): List<SkChannel.SkGroupChannel> {
    val userChannels = channelMemberDataSource.getChannelIdsForUserAndWorkspace(userId, workspaceId)
    return slackCloneDB.getCollection<SkChannel.SkGroupChannel>()
      .find(SkChannel.SkGroupChannel::uuid `in` userChannels)
      .toList()
  }

  override suspend fun getAllDMChannels(workspaceId: String, userId: String): List<SkChannel.SkDMChannel> {
    val userChannels = channelMemberDataSource.getChannelIdsForUserAndWorkspace(userId, workspaceId)
    return slackCloneDB.getCollection<SkChannel.SkDMChannel>()
      .find(SkChannel.SkDMChannel::uuid `in` userChannels)
      .toList()
  }

  override suspend fun checkIfDMChannelExists(userId: String, receiverId: String?): SkChannel.SkDMChannel? {
    return slackCloneDB.getCollection<SkChannel.SkDMChannel>()
      .findOne(SkChannel.SkDMChannel::senderId eq userId, SkChannel.SkDMChannel::receiverId eq receiverId)
      ?: slackCloneDB.getCollection<SkChannel.SkDMChannel>()
        .findOne(SkChannel.SkDMChannel::senderId eq receiverId, SkChannel.SkDMChannel::receiverId eq userId)
  }

  override suspend fun savePublicChannel(
    request: SkChannel.SkGroupChannel,
    adminId: String
  ): SkChannel.SkGroupChannel? {
    slackCloneDB.getCollection<SkChannel.SkGroupChannel>()
      .insertOne(request)
    slackCloneDB.getCollection<SkChannelMember>()
      .insertOne(
        SkChannelMember(
          workspaceId = request.workspaceId,
          channelId = request.channelId,
          memberId = adminId
        )
      )
    return slackCloneDB.getCollection<SkChannel.SkGroupChannel>()
      .findOne(SkChannel.SkGroupChannel::uuid eq request.uuid)
  }

  override suspend fun saveDMChannel(request: SkChannel.SkDMChannel): SkChannel.SkDMChannel? {
    slackCloneDB.getCollection<SkChannel.SkDMChannel>()
      .insertOne(request)
    slackCloneDB.getCollection<SkChannelMember>()
      .insertMany(
        hashSetOf(
          SkChannelMember(
            workspaceId = request.workspaceId,
            channelId = request.channelId,
            memberId = request.senderId
          ), SkChannelMember(
            workspaceId = request.workspaceId,
            channelId = request.channelId,
            memberId = request.receiverId
          )
        ).toMutableList()
      )
    return slackCloneDB.getCollection<SkChannel.SkDMChannel>()
      .findOne(SkChannel.SkDMChannel::uuid eq request.uuid)
  }

  override fun getDMChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel.SkDMChannel?, SkChannel.SkDMChannel?>> {
    val collection = slackCloneDB.getCollection<SkChannel.SkDMChannel>()

    val pipeline: List<Bson> = listOf(
      match(
        Document.parse("{'fullDocument.workspaceId': '$workspaceId'}"),
        Filters.`in`("operationType", OperationType.values().map { it.value }.toList())
      )
    )

    return collection
      .watch<SkChannel.SkDMChannel>(pipeline).toFlow().mapNotNull {
        Pair(it.fullDocumentBeforeChange, it.fullDocument)
      }
  }

  override fun getChannelChangeStream(workspaceId: String): Flow<Pair<SkChannel.SkGroupChannel?, SkChannel.SkGroupChannel?>> {
    val collection = slackCloneDB.getCollection<SkChannel.SkGroupChannel>()

    val pipeline: List<Bson> = listOf(
      match(
        Document.parse("{'fullDocument.workspaceId': '$workspaceId'}"),
        Filters.`in`("operationType", OperationType.values().map { it.value }.toList())
      )
    )
    return collection
      .watch<SkChannel.SkGroupChannel>(pipeline).toFlow().mapNotNull {
        Pair(it.fullDocumentBeforeChange, it.fullDocument)
      }
  }
}