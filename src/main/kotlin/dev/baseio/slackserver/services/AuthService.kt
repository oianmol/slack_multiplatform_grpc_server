package dev.baseio.slackserver.services


import dev.baseio.slackdata.common.Empty
import dev.baseio.slackdata.protos.*
import dev.baseio.slackserver.data.sources.AuthDataSource
import dev.baseio.slackserver.data.models.SkUser
import dev.baseio.slackserver.services.interceptors.*
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.Dispatchers
import java.security.Key
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext


class AuthService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    private val authDataSource: AuthDataSource,
    authenticationDelegate: AuthenticationDelegate
) :
    AuthServiceGrpcKt.AuthServiceCoroutineImplBase(coroutineContext), AuthenticationDelegate by authenticationDelegate {

    override suspend fun changePassword(request: SKAuthUser): Empty {
        return super.changePassword(request)
    }

    override suspend fun forgotPassword(request: SKAuthUser): SKUser {
        return super.forgotPassword(request)
    }

    override suspend fun resetPassword(request: SKAuthUser): SKUser {
        return super.resetPassword(request)
    }


}

fun jwtTokenFiveDays(generatedUser: SkUser?, key: Key): String? = Jwts.builder()
    .setClaims(hashMapOf<String, String?>().apply {
        put(USER_ID, generatedUser?.uuid)
        put(WORKSPACE_ID, generatedUser?.workspaceId)
    })
    .setExpiration(Date.from(Instant.now().plusMillis(TimeUnit.DAYS.toMillis(5))))// valid for 5 days
    .signWith(key)
    .compact()

fun skAuthResult(generatedUser: SkUser?): SKAuthResult {
    val keyBytes =
        Decoders.BASE64.decode(JWT_SIGNING_KEY)// TODO move this to env variables
    val key: Key = Keys.hmacShaKeyFor(keyBytes)
    val jws = jwtTokenFiveDays(generatedUser, key)
    return SKAuthResult.newBuilder()
        .setToken(jws) //no refresh token for now
        .build()
}