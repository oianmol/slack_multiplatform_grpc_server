package dev.baseio.slackserver.security

import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

/**
 * An implementation of [RsaEcdsaKeyManager] that supports RSA-ECDSA keys.
 */
 class RsaEcdsaKeyManager  constructor(
  chainId: String
) {
  val keychainId = "rsa_ecdsa_jvm$chainId"
  var keyStore: KeyStore = JVMSecurityProvider.loadKeyStore()


  init {
    if (!keyStore.containsAlias(keychainId)) {
      rawGenerateKeyPair()
    }
  }

   fun rawGenerateKeyPair() {
    JVMKeyStoreRsaUtils.generateKeyPair(keychainId)
  }

   fun rawGetPublicKey(): ByteArray {
    return JVMKeyStoreRsaUtils.getPublicKey(keychainId).encoded
  }


   fun decrypt(cipherText: ByteArray, privateKey: PrivateKey): ByteArray {
    //val verified = deserializeAndVerify(cipherText)
    return HybridRsaUtils.decrypt(
      cipherText, privateKey, RsaEcdsaConstants.Padding.OAEP,
      RsaEcdsaConstants.OAEP_PARAMETER_SPEC
    )
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
    JVMKeyStoreRsaUtils.deleteKeyPair(keyStore, keychainId)
  }

   fun getPrivateKey(): PrivateKey = JVMKeyStoreRsaUtils.getPrivateKey(keychainId)
   fun getPublicKey(): PublicKey = JVMKeyStoreRsaUtils.getPublicKey(keychainId)
   fun getPublicKeyFromBytes(publicKeyBytes: ByteArray): PublicKey {
    return JVMKeyStoreRsaUtils.getPublicKeyFromBytes(publicKeyBytes)
  }
}
