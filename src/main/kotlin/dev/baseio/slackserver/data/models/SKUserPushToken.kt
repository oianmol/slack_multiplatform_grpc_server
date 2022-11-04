package dev.baseio.slackserver.data.models

data class SKUserPushToken(val userId: String, val token: String, val platform: Int)


const val PLATFORM_ANDROID = 1
const val PLATFORM_IOS = 2