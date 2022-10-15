package dev.baseio.slackserver.data.impl

import dev.baseio.slackserver.data.sources.SkUser
import dev.baseio.slackserver.data.sources.SkWorkspace
import dev.baseio.slackserver.data.sources.WorkspaceDataSource
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.`in`

class WorkspaceDataSourceImpl(private val slackCloneDB: CoroutineDatabase) : WorkspaceDataSource {
    override suspend fun getWorkspaces(): List<SkWorkspace> {
        return slackCloneDB.getCollection<SkWorkspace>().find().toList()
    }

    override suspend fun findWorkspacesForEmail(email: String): List<SkWorkspace> {
        val workspaceIds = slackCloneDB.getCollection<SkUser>()
            .find(SkUser::email eq email)
            .toList().map {
                it.workspaceId
            }
        return slackCloneDB.getCollection<SkWorkspace>().find(SkWorkspace::uuid `in` workspaceIds)
            .toList()
    }

    override suspend fun findWorkspaceForName(name: String): SkWorkspace? {
        return slackCloneDB.getCollection<SkWorkspace>().findOne(SkWorkspace::name eq name)
    }

    override suspend fun saveWorkspace(skWorkspace: SkWorkspace): SkWorkspace? {
        slackCloneDB.getCollection<SkWorkspace>().insertOne(skWorkspace)
        return slackCloneDB.getCollection<SkWorkspace>().findOne(SkWorkspace::uuid eq skWorkspace.uuid)
    }
}