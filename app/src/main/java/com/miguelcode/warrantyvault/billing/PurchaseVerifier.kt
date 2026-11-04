package com.miguelcode.warrantyvault.billing

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Comprueba que una compra la ha firmado Google Play con la clave de licencias de la app
 * (Play Console > Monetizar > Configuración de monetización > Licencias).
 *
 * Sin esto, un cliente de billing falso (Lucky Patcher, hooks de Frida o Xposed) podía
 * devolver una compra "PURCHASED" inventada y la app daba premium. No sustituye a una
 * validación en servidor, pero obliga a falsificar también la firma RSA de Google.
 *
 * Falla cerrado: sin clave, o con una clave que no se puede leer, ninguna compra es
 * válida. La clave es pública, así que no es ningún secreto que haya que esconder.
 */
internal class PurchaseVerifier(base64PublicKey: String) {

    private val publicKey: PublicKey? = runCatching {
        val bytes = Base64.getDecoder().decode(base64PublicKey.trim())
        KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(bytes))
    }.getOrNull()

    val hasKey: Boolean get() = publicKey != null

    /**
     * @param signedData el JSON original de la compra (`Purchase.originalJson`).
     * @param signature la firma en base64 (`Purchase.signature`).
     */
    fun isAuthentic(signedData: String, signature: String): Boolean {
        val key = publicKey ?: return false
        if (signedData.isEmpty() || signature.isEmpty()) return false
        return runCatching {
            val signatureBytes = Base64.getDecoder().decode(signature)
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(key)
                update(signedData.toByteArray(Charsets.UTF_8))
                verify(signatureBytes)
            }
        }.getOrDefault(false)
    }

    private companion object {
        const val KEY_ALGORITHM = "RSA"
        const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    }
}
