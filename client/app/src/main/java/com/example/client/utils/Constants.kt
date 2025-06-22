package com.example.client.utils

/**
 * Contains server-related constants such as IP address, port, and route paths.
 */
object Server {
    //TODO: CHANGE IP
    /** IP address of the server. */
    const val SERVER_IP = ""
    /** Server port. */
    const val SERVER_PORT = "8000"
    /** Path of the user registration route on the server. */
    const val SERVER_REGISTER = "/users/add"
    /** Path of the vouchers' challenge route on the server. */
    const val SERVER_CHALLENGE_VOUCHERS = "/challenge/vouchers"
    /** Path of the transactions' challenge route on the server. */
    const val SERVER_CHALLENGE_TRANSACTIONS = "/challenge/transactions"
    /** Path of the vouchers route on the server. */
    const val SERVER_VOUCHERS = "/vouchers"
    /** Path of the transactions route on the server. */
    const val SERVER_TRANSACTIONS = "/transactions"
}

/**
 * Contains cryptography-related constants, including parameters for EC (Elliptic Curve) and RSA keys.
 */
object Crypto {
    /** Android Keystore provider name. */
    const val CRYPTO_ANDROID_KEYSTORE = "AndroidKeyStore"
    /** Serial number associated with the key. */
    const val CRYPTO_SERIAL_NR = 1234567890L

    // EC (Elliptic Curve) key related parameters
    /** EC (Elliptic Curve) key algorithm. */
    const val CRYPTO_EC_KEY_ALGO = "EC"
    /** EC curve used for EC key generation. */
    const val CRYPTO_EC_CURVE = "secp256r1"
    /** Signature algorithm for EC keys. */
    const val CRYPTO_EC_SIGN_ALGO = "SHA256withECDSA"
    /** Name used for the EC key in the keystore. */
    const val CRYPTO_EC_NAME = "myECDemoKey"

    // RSA key related parameters
    /** RSA key size. */
    const val CRYPTO_RSA_KEY_SIZE = 512
    /** RSA key algorithm. */
    const val CRYPTO_RSA_KEY_ALGO = "RSA"
    /** Signature algorithm used for RSA keys. */
    const val CRYPTO_RSA_SIGN_ALGO = "SHA256WithRSA"
    /** RSA encryption algorithm. */
    const val CRYPTO_RSA_ENC_ALGO = "RSA/NONE/PKCS1Padding"
    /** Name used for the RSA key in the keystore. */
    const val CRYPTO_RSA_NAME = "myRSADemoKey"
    /** Certificate serial number. */
    const val CRYPTO_CERT_SERIAL = 12121212
}

/**
 * Contains constants for the NFC communication.
 */
object NFC {
    /** Card processing completion action. */
    const val NFC_ACTION_CARD_DONE = "CMD_PROCESSING_DONE"
}