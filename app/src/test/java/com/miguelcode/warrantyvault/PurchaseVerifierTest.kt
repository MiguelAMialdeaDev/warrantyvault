package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.billing.PurchaseVerifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PurchaseVerifierTest {

    private val keys: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
    private val publicKey = Base64.getEncoder().encodeToString(keys.public.encoded)
    private val json = """{"packageName":"com.miguelcode.warrantyvault","productId":"premium_monthly"}"""

    private fun sign(data: String, pair: KeyPair = keys): String =
        Signature.getInstance("SHA1withRSA").run {
            initSign(pair.private)
            update(data.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(sign())
        }

    @Test
    fun `a purchase signed by Play is authentic`() {
        assertTrue(PurchaseVerifier(publicKey).isAuthentic(json, sign(json)))
    }

    @Test
    fun `a purchase whose data was altered is rejected`() {
        val signature = sign(json)
        val tampered = json.replace("premium_monthly", "premium_yearly")

        assertFalse(PurchaseVerifier(publicKey).isAuthentic(tampered, signature))
    }

    @Test
    fun `a purchase signed with another key is rejected`() {
        val otherKeys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()

        assertFalse(PurchaseVerifier(publicKey).isAuthentic(json, sign(json, otherKeys)))
    }

    @Test
    fun `without a key every purchase is rejected`() {
        val verifier = PurchaseVerifier("")

        assertFalse(verifier.hasKey)
        assertFalse(verifier.isAuthentic(json, sign(json)))
    }

    @Test
    fun `a malformed key or signature is rejected without crashing`() {
        assertFalse(PurchaseVerifier("esto no es base64").isAuthentic(json, sign(json)))
        assertFalse(PurchaseVerifier(publicKey).isAuthentic(json, "%%%"))
        assertFalse(PurchaseVerifier(publicKey).isAuthentic(json, ""))
    }
}
