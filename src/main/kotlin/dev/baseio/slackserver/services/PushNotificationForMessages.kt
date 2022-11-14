package dev.baseio.slackserver.services

import dev.baseio.slackserver.communications.PushNotificationSender
import dev.baseio.slackserver.data.models.SkMessage
import dev.baseio.slackserver.data.sources.ChannelMemberDataSource
import dev.baseio.slackserver.data.sources.UserPushTokenDataSource
import dev.baseio.slackserver.data.sources.UsersDataSource
import kotlinx.coroutines.launch

class PushNotificationForMessages(
  private val channelMemberDataSource: ChannelMemberDataSource,
  private val userPushTokenDataSource: UserPushTokenDataSource,
  private val usersDataSource: UsersDataSource
) : PushNotificationSender<SkMessage>() {

  override fun sendPushNotifications(request: SkMessage, senderUserId: String) {
    coroutineScope.launch {
      val sender = usersDataSource.getUser(senderUserId, request.workspaceId)!!
      val pushTokens = channelMemberDataSource.getMembers(request.workspaceId, request.channelId).map { it.memberId }
        .let { skChannelMembers ->
          userPushTokenDataSource.getPushTokensFor(skChannelMembers)
        }
      sendMessagesNow(pushTokens, request, sender)

    }
  }
}