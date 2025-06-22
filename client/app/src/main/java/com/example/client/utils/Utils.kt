package com.example.client.utils

import android.content.Context
import android.content.res.Configuration
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import com.example.client.R

// General top-level utility functions and data classes

/**
 * Configuration of the title in the toolbar to be black if dark mode is on.
 * @param activity Activity where the toolbar is defined.
 * @param toolbar Toolbar.
 */
fun configuratorToolbarTitle(activity : AppCompatActivity, toolbar: Toolbar){
    val darkModeFlags = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if(darkModeFlags == Configuration.UI_MODE_NIGHT_YES) {
        toolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.black));
        (toolbar.navigationIcon)?.setTint(ContextCompat.getColor(activity, R.color.black))
        toolbar.overflowIcon?.setTint(ContextCompat.getColor(activity, R.color.black))
    }
}

/**
 * Configuration of the menu in the toolbar to be black if dark mode is on.
 * @param activity Activity where the toolbar is defined.
 * @param menu Menu.
 */
fun configuratorMenu(activity: AppCompatActivity, menu: Menu?) {
    val iconColor = ContextCompat.getColor(activity, R.color.black)
    if(menu != null){
        for (i in 0 until menu.size) {
            menu[i].icon?.mutate()?.setTint(iconColor)
        }
    }
}

/**
 * Converts a byte array to a hexadecimal representation.
 *
 * This function takes an array of bytes and converts each byte to its corresponding hexadecimal value.
 * The final format is a string containing the hexadecimal values, with no spaces between them.
 *
 * @param ba ByteArray to convert to a hexadecimal string.
 * @return String representing the hexadecimal values of the given byte array.
 */
fun byteArrayToHex(ba: ByteArray): String {
    val sb = StringBuilder(ba.size * 2)
    for (b in ba) sb.append(String.format("%02x", b))
    return sb.toString()
}

/**
 * Converts a hexadecimal string to a byte array.
 *
 * This function takes a string representing data in hexadecimal format and converts it back to a byte array.
 * Each pair of hexadecimal characters (e.g. "1A", "FF") is converted to a corresponding byte.
 * The string must have an even number of characters.
 *
 * @param s Hexadecimal string to convert to a byte array.
 * @return Byte array equivalent to the given hexadecimal string.
 */
fun hexStringToByteArray(s: String): ByteArray {
    val data = ByteArray(s.length/2)
    for (k in 0 until s.length/2)
        data[k] = ((Character.digit(s[2*k], 16) shl 4) + Character.digit(s[2*k+1], 16)).toByte()
    return data
}

/**
 * Reads the contents of an InputStream and returns it as a string.
 *
 * This function reads line by line from an input stream and stores the contents in a string.
 * If an error occurs while trying to read from the stream, the function catches the exception and returns an error message in string.
 *
 * @param input Input stream (InputStream) to read from.
 * @return Contents read from the input stream as a string. If there is an error, it returns an error message.
 */
fun readStream(input: InputStream): String {
    var reader: BufferedReader? = null
    var line: String?
    val response = StringBuilder()
    try {
        reader = BufferedReader(InputStreamReader(input))
        while (reader.readLine().also{ line = it } != null)
            response.append(line)
    }
    catch (e: IOException) {
        response.clear()
        response.append("readStream: ${e.message}")
    }
    reader?.close()
    return response.toString()
}

fun Context.isDarkThemeOn(): Boolean {
    val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
}
