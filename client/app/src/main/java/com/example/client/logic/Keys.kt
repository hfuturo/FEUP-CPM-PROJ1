package com.example.client.logic

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.client.utils.Crypto.CRYPTO_ANDROID_KEYSTORE
import com.example.client.utils.Crypto.CRYPTO_EC_CURVE
import com.example.client.utils.Crypto.CRYPTO_EC_KEY_ALGO
import com.example.client.utils.Crypto.CRYPTO_EC_NAME
import com.example.client.utils.Crypto.CRYPTO_RSA_KEY_ALGO
import com.example.client.utils.Crypto.CRYPTO_RSA_KEY_SIZE
import com.example.client.utils.Crypto.CRYPTO_RSA_NAME
import com.example.client.utils.Crypto.CRYPTO_SERIAL_NR
import com.example.client.utils.Crypto.CRYPTO_CERT_SERIAL
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Calendar
import java.util.GregorianCalendar
import javax.security.auth.x500.X500Principal

/**
 * Generates a pair of EC (Elliptic Curve) cryptographic keys and stores them securely in the Android Keystore.
 *
 * This method creates a private key and a public key of type EC using the curve defined in `Crypto.EC_CURVE`.
 * The generated key is stored in the Android Keystore to ensure storage security.
 *
 * @return Returns `true` if key generation and storage were successful, or `false` on error.
 */
fun generateEC() : Boolean {
    try {
        val spec = KeyGenParameterSpec.Builder(CRYPTO_EC_NAME, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CRYPTO_EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setCertificateSubject(X500Principal("CN=$CRYPTO_EC_NAME"))
            .setCertificateSerialNumber(BigInteger.valueOf(CRYPTO_SERIAL_NR))
            .setCertificateNotBefore(GregorianCalendar().time)
            .setCertificateNotAfter(GregorianCalendar().apply { add(Calendar.YEAR, 10) }.time)
            .build()
        KeyPairGenerator.getInstance(CRYPTO_EC_KEY_ALGO, CRYPTO_ANDROID_KEYSTORE).apply {
            initialize(spec)
            generateKeyPair()
        }
    }
    catch(_: Exception) {
        return false
    }
    return true
}

/**
 * Generates a pair of RSA cryptographic keys and stores them securely in the Android Keystore.
 *
 * This method creates a private key and a public key of type RSA with the key size defined in `Crypto.RSA_KEY_SIZE`.
 * The generated key is stored in the Android Keystore.
 *
 * @return Returns `true` if key generation and storage were successful, or `false` on error.
 */
fun generateRSA(): Boolean {
    try {
        val spec = KeyGenParameterSpec.Builder(CRYPTO_RSA_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setKeySize(CRYPTO_RSA_KEY_SIZE)
            .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=$CRYPTO_RSA_NAME"))
            .setCertificateSerialNumber(BigInteger.valueOf(CRYPTO_CERT_SERIAL.toLong()))
//            .setCertificateSerialNumber(BigInteger.valueOf(CRYPTO_SERIAL_NR))
            .setCertificateNotBefore(GregorianCalendar().time)
            .setCertificateNotAfter(GregorianCalendar().apply { add(Calendar.YEAR, 10) }.time)
            .build()
        KeyPairGenerator.getInstance(CRYPTO_RSA_KEY_ALGO, CRYPTO_ANDROID_KEYSTORE).apply {
            initialize(spec)
            generateKeyPair()
        }
    }
    catch (_: Exception) {
        return false
    }
    return true
}

/**
 * Retrieves the public key from a key entry of type `PrivateKeyEntry` in the Android Keystore.
 *
 * @param entry Key entry (of type `PrivateKeyEntry`) that contains the public and private key.
 * @return Public key associated with the given key entry.
 */
fun getPublicKey(entry: KeyStore.PrivateKeyEntry?): PublicKey? {
    return entry?.certificate?.publicKey
}

/**
 * Retrieves the private key from a key entry of type `PrivateKeyEntry` in the Android Keystore.
 *
 * @param entry Key entry (of type `PrivateKeyEntry`) that contains the public and private key.
 * @return Private key associated with the given key entry.
 */
fun getPrivateKey(entry: KeyStore.PrivateKeyEntry?): PrivateKey? {
    return entry?.privateKey
}