package com.example.terminal

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.example.terminal.utils.NFC.NFC_CARD_AID
import com.example.terminal.utils.NFC.NFC_CMD_SEL_AID
import com.example.terminal.utils.hexStringToByteArray
import java.io.IOException

private val RES_OK_SW = hexStringToByteArray("9000")

/**
 * NFCReader class for reading NFC tags using the Android IsoDep protocol.
 *
 * @param listener A lambda function that takes two parameters:
 *   - `Int`: The first byte of the result indicating the response code.
 *   - `ByteArray`: The data returned by the NFC tag, excluding the response code and status bytes.
 */
class NFCReader(private val listener: (Int, ByteArray)->Unit) : NfcAdapter.ReaderCallback {

  /**
   * This method is called when a tag is discovered.
   * It connects to the NFC tag and sends a 'select AID' command to it, expecting a response.
   * If the response is valid (indicated by a specific status code), the result is passed to the listener.
   *
   * @param tag The NFC tag that was discovered.
   */
  override fun onTagDiscovered(tag: Tag) {
    // Android smart card reader emulator
    val isoDep = IsoDep.get(tag)

    if (isoDep != null) {
      try {

        // establish a connection with the card and send 'select aid' command
        isoDep.connect()

        // send the 'select AID' command with the card AID
        val result = isoDep.transceive(
          hexStringToByteArray(
            NFC_CMD_SEL_AID + String.format(
              "%02X",
              NFC_CARD_AID.length / 2
            ) + NFC_CARD_AID
          )
        )
        val rLen = result.size

        // extract status bytes and check if it is the expected
        val status = byteArrayOf(result[rLen-2], result[rLen-1])
        if (RES_OK_SW.contentEquals(status)) {
          listener(
            result[0].toInt(),
            result.sliceArray(1..rLen - 3)
          )
        }
      }
      catch (e: IOException) {
        Log.e("CardReader", "Error communicating with card: $e")
      }
    }
  }
}