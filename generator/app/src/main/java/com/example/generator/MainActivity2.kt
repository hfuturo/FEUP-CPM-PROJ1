package com.example.generator

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.generator.utils.setInsetsPadding
import java.nio.charset.StandardCharsets
import java.util.Hashtable
import kotlin.collections.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/**
 * Activity responsible for displaying a QR code generated from encrypted content.
 * The activity receives data in byte array format, converts it into text, and generates a QR code.
 */
class MainActivity2 : AppCompatActivity() {

    // UI elements
    private val qrCodeImageview by lazy { findViewById<ImageView>(R.id.img_qr_code) }
    private val tvError by lazy { findViewById<TextView>(R.id.tv_error) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContentView(R.layout.activity_main2)
        setInsetsPadding(tvError, bottom=0)

        // get the encrypted data passed from the previous activity
        val content = intent.getByteArrayExtra("data")!!
        val qrContent = String(content, StandardCharsets.ISO_8859_1)

        // create the QR code in a separate thread to avoid ANR (Application Not Responding)
        Thread {
            try {
                val bitmap = encodeAsBitmap(qrContent)
                runOnUiThread {
                    qrCodeImageview.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // display error if something goes wrong
                tvError.text = e.message
            }
        }.start()
    }

    /**
     * Converts the given string into a QR code and returns a Bitmap representing the generated QR code.
     *
     * @param str The text to be encoded into the QR code.
     * @return The generated QR code as a Bitmap, or `null` if an error occurs during generation.
     */
    private fun encodeAsBitmap(str: String): Bitmap? {
        // define the dimensions of the QR code (1000x1000 pixels)
        val DIMENSION = 1000

        // define encoding parameters, including the character set used
        val hints = Hashtable<EncodeHintType, String>().also {
            it[EncodeHintType.CHARACTER_SET] = "ISO-8859-1"
        }

        // try to generate the QR code from the provided text
        val result = try {
            MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, DIMENSION, DIMENSION, hints)
        } catch (iae: IllegalArgumentException) {
            tvError.text = iae.message
            return null
        }

        // convert the result to a Bitmap
        val w = result.getWidth()
        val h = result.getHeight()
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        // create and return the Bitmap
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }
}