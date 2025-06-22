package com.example.terminal.utils

import android.nfc.NfcAdapter

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
    const val SERVER_PAY = "/pay"
}

/**
 * Contains constants for the NFC communication.
 */
object NFC {
    /** NFC card AID (Application Identifier). */
    const val NFC_CARD_AID = "F010203040"
    /** APDU command to select the card AID. */
    const val NFC_CMD_SEL_AID = "00A40400"
    /** Flags used to configure the NFC reader. */
    const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
}