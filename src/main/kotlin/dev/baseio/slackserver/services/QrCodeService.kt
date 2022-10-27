package dev.baseio.slackserver.services

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import dev.baseio.slackdata.protos.*
import dev.baseio.slackserver.data.models.SkUser
import dev.baseio.slackserver.services.interceptors.AUTH_CONTEXT_KEY
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream


val qrCodeGenerator = QrCodeGenerator()

class QrCodeService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    private val database: CoroutineDatabase,
) : QrCodeServiceGrpcKt.QrCodeServiceCoroutineImplBase(coroutineContext) {

    override fun generateQRCode(request: SKQrCodeGenerator): Flow<SKQrCodeResponse> {
        return channelFlow {
            val data = UUID.randomUUID().toString()
            val result = qrCodeGenerator.process(data)
            send(result.first)
            qrCodeGenerator.inMemoryQrCodes[data] = Pair(result.second) {
                launch {
                    send(sKQrCodeResponse {
                        this.authResult = it
                    })
                }
            }
            awaitClose {
                qrCodeGenerator.inMemoryQrCodes[data]?.first?.deleteIfExists()
                qrCodeGenerator.inMemoryQrCodes.remove(data)
            }
        }
    }


    override suspend fun verifyQrCode(request: SKQRAuthVerify): SKAuthResult {
        qrCodeGenerator.inMemoryQrCodes[request.token]?.let {
            val user = AUTH_CONTEXT_KEY.get()
            val skUser = database.getCollection<SkUser>().findOne(SkUser::uuid eq user.userId)
            it.first.deleteIfExists()
            val result = skAuthResult(skUser)
            qrCodeGenerator.notifyAuthenticated(result, request)
            return result
        }
        throw StatusException(Status.UNAUTHENTICATED)
    }
}

class QrCodeGenerator {
    val inMemoryQrCodes = hashMapOf<String, Pair<Path, (SKAuthResult) -> Unit>>() // TODO this is dirty!

    fun process(data: String): Pair<SKQrCodeResponse, Path> {
        with(generateImage(data)) {
            val ins = inputStream(java.nio.file.StandardOpenOption.READ)
            val bytes = ins.readAllBytes()
            val intBytes = bytes.map { it.toInt() }
            return Pair(sKQrCodeResponse {
                this.byteArray.addAll(intBytes.map { sKByteArrayElement { this.byte = it } })
                this.totalSize = fileSize()
            }.also {
                ins.close()
            }, this)
        }
    }

    private fun generateImage(data: String): Path {
        val validTill = LocalDateTime.now().plusSeconds(120)
        val matrix: BitMatrix = MultiFormatWriter().encode(
            data,
            BarcodeFormat.QR_CODE, 512, 512
        )
        val path = File.createTempFile(data, validTill.toString()).toPath()
        MatrixToImageWriter.writeToPath(matrix, "png", path).apply {
            return path
        }
    }

    suspend fun notifyAuthenticated(result: SKAuthResult, request: SKQRAuthVerify) {
        qrCodeGenerator.inMemoryQrCodes[request.token]?.first?.deleteIfExists()
        qrCodeGenerator.inMemoryQrCodes[request.token]?.second?.invoke(result)
        qrCodeGenerator.inMemoryQrCodes.remove(request.token)
    }

    fun registerFor(data: String, function: () -> Unit) {
        TODO("Not yet implemented")
    }
}
