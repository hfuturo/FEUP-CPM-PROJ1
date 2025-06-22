package com.example.generator.utils

/**
 * Contains server-related constants such as IP address, port, and route paths.
 */
object Server {
    //TODO: CHANGE IP
    /** IP address of the server. */
    const val SERVER_IP = ""
    /** Server port. */
    const val SERVER_PORT = "8000"
    /** Path of the informative action route on the server. */
    const val SERVER_INFORM = "/key"
    /** Path of the groceries route on the server. */
    const val SERVER_GROCERIES = "/groceries"
}

/**
 * Contains cryptography-related constants, including parameters RSA keys.
 */
object Crypto {
    /** Android Keystore provider name. */
    const val CRYPTO_ANDROID_KEYSTORE = "AndroidKeyStore"
    /** Serial number associated with the key. */
    const val CRYPTO_SERIAL_NUMBER = 1234567890L
    /** RSA key size. */
    const val CRYPTO_KEY_SIZE = 512
    /** RSA key algorithm. */
    const val CRYPTO_KEY_ALGO = "RSA"
    /** Signature algorithm used for RSA keys. */
    const val CRYPTO_SIGN_ALGO = "SHA256WithRSA"
    /** RSA encryption algorithm. */
    const val CRYPTO_ENC_ALGO = "RSA/NONE/PKCS1Padding"
    /** Name used for the RSA key in the keystore. */
    const val CRYPTO_NAME = "AcmeKey"
    /** Unique identifier (tag) for the key. */
    const val CRYPTO_TAG_ID = 0x41636D65
    /** Certificate serial number. */
    const val CRYPTO_CERT_SERIAL = 12121212
    /** Card processing completion action. */
    const val CRYPTO_ACTION_CARD_DONE = "CMD_PROCESSING_DONE"
}