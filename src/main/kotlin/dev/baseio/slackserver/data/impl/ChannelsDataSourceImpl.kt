package dev.baseio.slackserver.data.impl

import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.OperationType
import dev.baseio.slackserver.data.sources.ChannelsDataSource
import dev.baseio.slackserver.data.sources.SkChannel
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.match

class ChannelsDataSourceImpl(private val slackCloneDB: CoroutineDatabase) : ChannelsDataSource {
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
        Pair(it.fullDocumentBeforeChange,it.fullDocument)
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