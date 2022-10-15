package dev.baseio.slackserver.data.impl

import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.OperationType
import dev.baseio.slackserver.data.sources.SkUser
import dev.baseio.slackserver.data.sources.UsersDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.match

class UsersDataSourceImpl(private val slackCloneDB: CoroutineDatabase) : UsersDataSource {
    override suspend fun getUser(userId: String, workspaceId: String): SkUser? {
        return slackCloneDB.getCollection<SkUser>()
            .findOne(SkUser::uuid eq userId, SkUser::workspaceId eq workspaceId)
    }

    override suspend fun saveUser(skUser: SkUser): SkUser? {
        slackCloneDB.getCollection<SkUser>()
            .insertOne(skUser)
        return slackCloneDB.getCollection<SkUser>().findOne(SkUser::uuid eq skUser.uuid)
    }

    override fun getChangeInUserFor(workspaceId: String): Flow<Pair<SkUser?, SkUser?>> {
        val collection = slackCloneDB.getCollection<SkUser>()

        val pipeline: List<Bson> = listOf(
            match(
                Document.parse("{'fullDocument.workspaceId': '$workspaceId'}"),
                Filters.`in`("operationType", OperationType.values().map { it.value }.toList())
            )
        )

        return collection
            .watch<SkUser>(pipeline).toFlow().map {
                Pair(it.fullDocumentBeforeChange,it.fullDocument)
            }
    }

    override suspend fun getUsers(workspaceId: String): List<SkUser> {
        return slackCloneDB.getCollection<SkUser>()
            .find(SkUser::workspaceId eq workspaceId).toList()
    }
}