package com.example.client.logic

import android.annotation.SuppressLint
import android.util.Log
import com.example.client.utils.Server.SERVER_CHALLENGE_VOUCHERS
import com.example.client.utils.Server.SERVER_CHALLENGE_TRANSACTIONS
import com.example.client.utils.Server.SERVER_IP
import com.example.client.utils.Server.SERVER_PORT
import com.example.client.utils.Server.SERVER_REGISTER
import com.example.client.utils.Server.SERVER_VOUCHERS
import com.example.client.utils.Server.SERVER_TRANSACTIONS
import com.example.client.utils.readStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Converts a public key of type `PublicKey` into a Base64 encoded string.
 *
 * @param publicKey Public key to convert.
 * @return Public key in Base64 format as a string. Returns an empty string if key is null.
 */
fun publicKeyToBase64(publicKey: PublicKey?): String {
    if (publicKey == null)
        return ""
    return Base64.getEncoder().encodeToString(publicKey.encoded)
}

/**
 * Converts a public key Base64 string back into a `PublicKey` object.
 *
 * @param base64Key Base64 encoded public key.
 * @return Public key of type `PublicKey`, or `null` if an error occurs during conversion.
 */
fun base64ToPublicKey(base64Key: String): PublicKey? {
    return try {
        val keyBytes = Base64.getDecoder().decode(base64Key)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        keyFactory.generatePublic(keySpec)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Performs the request for the challenge (Nonce).
 * @param uuid Uuid of the user.
 * @return String containing the server response or an error message.
 */
suspend fun actionChallengeVouchers(uuid: String) : String {
    val url = "http://${SERVER_IP}:${SERVER_PORT}${SERVER_CHALLENGE_VOUCHERS}"
    val payload = "{\"user\": \"$uuid\"}"
    return sendMessageServer(url, payload)
}

/**
 * Performs the fetch of the user's vouchers
 *
 * @param uuid Uuid of the user.
 * @param message Encrypted nonce.
 * @return String containing the server response or an error message.
 */
suspend fun actionGetVouchers(uuid: String, message : ByteArray) : String {
    val url = "http://${SERVER_IP}:${SERVER_PORT}${SERVER_VOUCHERS}"
    val messageBase64 = android.util.Base64.encodeToString(message, android.util.Base64.NO_WRAP)
    val payload = "{\"user\": \"$uuid\", \"message\": \"$messageBase64\"}"
    return sendMessageServer(url, payload)
}

suspend fun actionChallengeTransactions(uuid: String): String {
    val url = "http://${SERVER_IP}:${SERVER_PORT}${SERVER_CHALLENGE_TRANSACTIONS}"
    val payload = "{\"user\": \"$uuid\"}"
    return sendMessageServer(url, payload)
}

suspend fun actionGetTransactions(uuid: String, message: ByteArray): String {
    val url = "http://${SERVER_IP}:${SERVER_PORT}${SERVER_TRANSACTIONS}"
    Log.d("Test", message.size.toString())
    val messageBase64 = android.util.Base64.encodeToString(message, android.util.Base64.NO_WRAP)
    val payload = "{\"user\": \"$uuid\", \"message\": \"$messageBase64\"}"
    return sendMessageServer(url, payload)
}

/**
 * Performs the user registration process on the server.
 *
 * @param publicEC User's EC (Elliptic Curve) public key.
 * @param publicRSA User's RSA public key.
 * @param name Name of the user.
 * @param nick Nickname of the user.
 * @param cardNumber Number (in string) of the user's card.
 * @param cardDate Expiration Date (in string) of the user's card.
 * @param selectedCardType Type of the user's card
 * @return String containing the server response or an error message.
 */
@SuppressLint("SimpleDateFormat")
suspend fun actionRegistration(publicEC: PublicKey?, publicRSA: PublicKey?, cardNumber : String, cardDate : String, selectedCardType : String): String {
    try {
        val publicStringEC = publicKeyToBase64(publicEC)
        val publicStringRSA = publicKeyToBase64(publicRSA)

        val url = "http://${SERVER_IP}:${SERVER_PORT}${SERVER_REGISTER}"
        val payload = "{\"keyEC\": \"$publicStringEC\", \"keyRSA\": \"$publicStringRSA\", \"cardNumber\": \"$cardNumber\", \"cardDate\": \"$cardDate\", \"selectedCardType\": \"$selectedCardType\"}"
        val result = sendMessageServer(url, payload)
        return result
    }
    catch (_: Exception) {
        return "Error - The server was not available. Try again!"
    }
}

/**
 * This method makes an HTTP `POST` request to the server.
 * @param url Url with the route for the request
 * @param payload Content of the request
 * @return String containing the server response or an error message.
 */
suspend fun sendMessageServer(url: String, payload: String): String {
    return withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        var result: String
        try {
            urlConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                doOutput = true
                doInput = true
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                useCaches = false
                connectTimeout = 5000
                with(outputStream) {
                    write(payload.toByteArray())
                    flush()
                    close()
                }
                // get response
                result = if (responseCode == 200)
                    readStream(inputStream)
                else
                    "Code: $responseCode"
            }
        } catch (e: Exception) {
            result = e.toString()
        }
        urlConnection?.disconnect()
        result
    }
}
