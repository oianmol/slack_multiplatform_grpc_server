package dev.baseio.slackserver.security

import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

/**
 * Contains common helper functions used by the Android classes.
 */
object JVMSecurityProvider {
    const val KEYSTORE_JVM = "JKS"

    fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(KEYSTORE_JVM)
        try {
            keyStore.load(null)
        } catch (e: IOException) {
            throw GeneralSecurityException("unable to load keystore", e)
        }
        return keyStore
    }
}
