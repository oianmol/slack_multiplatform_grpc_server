package dev.baseio.slackserver.communications

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import dev.baseio.slackserver.data.models.IDataMap
import dev.baseio.slackserver.data.models.SKUserPushToken
import dev.baseio.slackserver.data.models.SkUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class PushNotificationSender<T : IDataMap> {
  protected val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  abstract fun sendPushNotifications(request: T, senderUserId: String)

  private fun toFirebaseMessage(
    model: T,
    userToken: String,
    resourceName: String
  ): Message {
    val dataMap = model.provideMap()
    return Message.builder()
      .setToken(userToken)
      .setAndroidConfig(AndroidConfig.builder().putAllData(dataMap).build())
      .setNotification(
        Notification.builder()
          .setBody("Notification from $resourceName")
          .setTitle(dataMap["type"]).build()
      )
      .build()
  }

  fun sendMessagesNow(pushTokens: List<SKUserPushToken>, request: T, sender: SkUser) {
    FirebaseMessaging.getInstance().sendAll(pushTokens.map { skUserPushToken ->
      toFirebaseMessage(request, skUserPushToken.token, sender.name)
    })
  }
}