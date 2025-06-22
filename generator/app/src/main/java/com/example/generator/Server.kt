package com.example.generator

import com.example.generator.utils.Server.SERVER_GROCERIES
import com.example.generator.utils.Server.SERVER_IP
import com.example.generator.utils.Server.SERVER_PORT
import com.example.generator.utils.readStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Performs the fetch of the groceries of the store.
 *
 * @return String containing the server response or an error message.
 */
suspend fun getGroceries(): String {
    val url = "http://${SERVER_IP}:${SERVER_PORT}${SERVER_GROCERIES}"
    return sendMessageServer(url)
}

/**
 * Makes an HTTP GET request to the server.
 *
 * @param url URL with the route for the request
 * @return String containing the server response or an error message.
 */
suspend fun sendMessageServer(url: String): String {
    return withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        var result: String

        try {
            urlConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                doInput = true
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                useCaches = false
                connectTimeout = 5000
                result = if (responseCode == 200) {
                    readStream(inputStream)
                } else {
                    "Code: $responseCode"
                }
            }
        } catch (e: Exception) {
            result = e.toString()
        }

        urlConnection?.disconnect()
        result
    }
}