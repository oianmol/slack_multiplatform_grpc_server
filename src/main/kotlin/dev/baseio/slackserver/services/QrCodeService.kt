package dev.baseio.slackserver.services

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import dev.baseio.slackdata.protos.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

class QrCodeService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
) : QrCodeServiceGrpcKt.QrCodeServiceCoroutineImplBase(coroutineContext) {
    override suspend fun generateQRCode(request: SKQrCodeGenerator): SKQrCodeResponse {
        with(generateImage()) {
            val ins = inputStream(StandardOpenOption.READ)
            val bytes = ins.readAllBytes()
            val intBytes = bytes.map { it.toInt() }
            return sKQrCodeResponse {
                this.byteArray.addAll(intBytes.map { sKByteArrayElement { this.byte = it } })
                this.totalSize = fileSize()
            }.also {
                ins.close()
            }
        }
    }

    private fun generateImage(): Path {
        val validTill = LocalDateTime.now().plusSeconds(120)
        val data = UUID.randomUUID().toString()
        val matrix: BitMatrix = MultiFormatWriter().encode(
            data,
            BarcodeFormat.QR_CODE, 512, 512
        )
        val path = File.createTempFile(data, validTill.toString()).toPath()
        MatrixToImageWriter.writeToPath(matrix, "png", path).apply {
            return path
        }
    }

    override suspend fun verifyQrCode(request: SKQRAuthVerify): SKAuthResult {
        return super.verifyQrCode(request)
    }
}