package dev.baseio.slackserver.communications

enum class NotificationType(val type: String, val titleMessage: String, val bodyMessage: String) {
    CHANNEL_CREATED(
        type = "channel_created",
        bodyMessage = "A new channel %s was created",
        titleMessage = "New Group Message Channel!"
    ),
    DM_CHANNEL_CREATED(
        type = "dm_channel_created",
        bodyMessage = "A new conversation was initiated by %s",
        titleMessage = "New Direct Message Channel!"
    ),
    ADDED_CHANNEL(
        type = "invited_channel",
        titleMessage = "Added to Channel",
        bodyMessage = "You were added to a slack channel by %s"
    ),
    NEW_MESSAGE(
        type = "new_message",
        titleMessage = "New Message",
        bodyMessage = "You have received a new message. %s"
    )
}