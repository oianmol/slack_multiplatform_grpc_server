package dev.baseio.slackserver.security

import java.security.PrivateKey
import java.security.PublicKey

/**
 * An implementation of [RsaEcdsaKeyManager] that supports RSA-ECDSA keys.
 */
class RsaEcdsaKeyManager constructor(
  chainId: String
) {
  private val keychainId = "rsa_ecdsa_jvm$chainId"

  init {
    rawGenerateKeyPair()
  }

  private fun rawGenerateKeyPair() {
    JVMKeyStoreRsaUtils.generateKeyPair(keychainId)
  }

  fun encrypt(plainData: ByteArray, publicKeyBytes: PublicKey): ByteArray {
    return HybridRsaUtils.encrypt(
      plainData,
      publicKeyBytes,
      RsaEcdsaConstants.Padding.OAEP,
      RsaEcdsaConstants.OAEP_PARAMETER_SPEC
    )
  }

  fun rawDeleteKeyPair() {
    JVMKeyStoreRsaUtils.deleteKeyPair(keychainId)
  }

  fun getPrivateKey(): PrivateKey = JVMKeyStoreRsaUtils.getPrivateKey(keychainId)
  fun getPublicKey(): PublicKey = JVMKeyStoreRsaUtils.getPublicKey(keychainId)
}
