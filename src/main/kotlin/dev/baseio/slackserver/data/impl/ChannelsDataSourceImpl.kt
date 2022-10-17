package dev.baseio.slackserver.data.impl

import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.OperationType
import dev.baseio.slackserver.data.sources.ChannelsDataSource
import dev.baseio.slackserver.data.models.SkChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.eq
import org.litote.kmongo.match

class ChannelsDataSourceImpl(
  private val slackCloneDB: CoroutineDatabase
) : ChannelsDataSource {

  override suspend fun getAllChannels(workspaceId: String): List<SkChannel.SkGroupChannel> {
    return slackCloneDB.getCollection<SkChannel.SkGroupChannel>().find(
      SkChannel.SkGroupChannel::workspaceId eq workspaceId
    ).toList()
  }

  override suspend fun getAllDMChannels(workspaceId: String): List<SkChannel.SkDMChannel> {
    return slackCloneDB.getCollection<SkChannel.SkDMChannel>().find(
      SkChannel.SkDMChannel::workspaceId eq workspaceId
    ).toList()
  }

  override suspend fun getChannel(uuid: String, workspaceId: String): SkChannel.SkGroupChannel? {
    return slackCloneDB.getCollection<SkChannel.SkGroupChannel>()
      .findOne(SkChannel.SkGroupChannel::uuid eq uuid, SkChannel.SkGroupChannel::workspaceId eq workspaceId)
  }

  override suspend fun getDMChannel(uuid: String, workspaceId: String): SkChannel.SkDMChannel? {
    return slackCloneDB.getCollection<SkChannel.SkDMChannel>()
      .findOne(SkChannel.SkDMChannel::uuid eq uuid, SkChannel.SkDMChannel::workspaceId eq workspaceId)

  }

  override suspend fun savePublicChannel(request: SkChannel.SkGroupChannel): SkChannel.SkGroupChannel? {
    slackCloneDB.getCollection<SkChannel.SkGroupChannel>()
      .insertOne(request)
    return slackCloneDB.getCollection<SkChannel.SkGroupChannel>()
      .findOne(SkChannel.SkGroupChannel::uuid eq request.uuid)
  }

  override suspend fun saveDMChannel(request: SkChannel.SkDMChannel): SkChannel.SkDMChannel? {
    slackCloneDB.getCollection<SkChannel.SkDMChannel>()
      .insertOne(request)
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