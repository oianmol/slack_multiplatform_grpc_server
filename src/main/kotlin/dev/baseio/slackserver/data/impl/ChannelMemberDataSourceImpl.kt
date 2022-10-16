package dev.baseio.slackserver.data.impl

import dev.baseio.slackserver.data.models.SkChannel
import dev.baseio.slackserver.data.models.SkChannelMember
import dev.baseio.slackserver.data.sources.ChannelMemberDataSource
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

class ChannelMemberDataSourceImpl(private val database: CoroutineDatabase) : ChannelMemberDataSource {
  override suspend fun isChannelExistFor(sender: String, receiver: String): SkChannel? {
    val possible1 = database.getCollection<SkChannelMember>()
      .findOne(SkChannelMember::channelId eq sender + receiver)
    val possible2 = database.getCollection<SkChannelMember>()
      .findOne(SkChannelMember::channelId eq receiver + sender)
    possible1?.let {
      return database.getCollection<SkChannel>().findOne(SkChannel::uuid eq it.channelId)
    }
    possible2?.let {
      return database.getCollection<SkChannel>().findOne(SkChannel::uuid eq it.channelId)
    }
    return null

  }

  override suspend fun getMembers(workspaceId: String, channelId: String): List<SkChannelMember> {
    return database.getCollection<SkChannelMember>()
      .find(SkChannelMember::channelId eq channelId, SkChannelMember::workspaceId eq workspaceId)
      .toList()
  }

  override suspend fun addMembers(listOf: List<SkChannelMember>) {
    listOf.forEach {
      database.getCollection<SkChannelMember>()
        .insertOne(SkChannelMember(channelId = it.channelId, memberId = it.memberId, workspaceId = it.workspaceId))
    }
  }

}