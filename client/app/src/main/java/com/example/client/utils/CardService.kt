package com.example.client.utils

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.client.utils.NFC.NFC_ACTION_CARD_DONE

object Card {

  var contentMessage = ByteArray(0)
  var type = 0

  // AID for this applet service.
  private const val CARD_AID = "F010203040"
  // SmartCard select AID command
  private const val CMD_SEL_AID = "00A40400"

  // SELECT AID command
  val SELECT_APDU = hexStringToByteArray(CMD_SEL_AID + String.format("%02X", CARD_AID.length/2) + CARD_AID)
  // "OK" status word (0x9000)
  val OK_SW = hexStringToByteArray("9000")
  // "UNKNOWN" command status word (0X0000)
  val UNKNOWN_CMD_SW = hexStringToByteArray("0000")
}

class CardService : HostApduService() {
  override fun processCommandApdu(command: ByteArray, extra: Bundle?): ByteArray {
    return if (Card.type != 0 && Card.SELECT_APDU.contentEquals(command))
      byteArrayOf(Card.type.toByte()) + Card.contentMessage + Card.OK_SW
    else
      Card.UNKNOWN_CMD_SW
  }

  /**
   * Notify the NFCSendActivity that the Card has finished.
   */
  override fun onDeactivated(cause: Int) {
    val localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    val broadcastIntent = Intent(NFC_ACTION_CARD_DONE)
    localBroadcastManager.sendBroadcast(broadcastIntent)
  }
}