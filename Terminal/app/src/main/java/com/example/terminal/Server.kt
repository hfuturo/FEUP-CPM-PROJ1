package com.example.terminal

import android.util.Base64
import android.util.Log
import com.example.terminal.utils.Server.SERVER_IP
import com.example.terminal.utils.Server.SERVER_PAY
import com.example.terminal.utils.Server.SERVER_PORT
import com.example.terminal.utils.readStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends a payment message to a server for verification and returns the result.
 *
 * @param message The payment message as a `ByteArray` to be sent to the server.
 * @return A string indicating the result of the verification.
 */
suspend fun pay(message : ByteArray): String {

    val url = URL("http://${SERVER_IP}:${SERVER_PORT}${SERVER_PAY}")
    val messageBase64 = Base64.encodeToString(message, Base64.NO_WRAP)
    val payload = "{\"message\": \"$messageBase64\"}"

    return withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        var result: String
        try {
            urlConnection = (url.openConnection() as HttpURLConnection).apply {
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

                val jsonResponse = JSONObject(result)
                val verified = jsonResponse.getBoolean("verified")

                if (verified) result = "True"
                else result = "False"
            }
        } catch (e: Exception) {
            Log.d("error", e.toString())
            result = e.toString()
        }

        urlConnection?.disconnect()
        result
    }
}
