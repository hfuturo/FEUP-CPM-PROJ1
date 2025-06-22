package com.example.client.logic

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.data.DiscountDB
import com.example.client.data.VouchersDB
import com.example.client.utils.Crypto.CRYPTO_RSA_ENC_ALGO
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.Cipher

lateinit var vouchersDB : VouchersDB
lateinit var discountDB: DiscountDB

/**
 * Data class representing a voucher.
 * @property id Unique identifier of the product.
 * @property value Value of the voucher (15%).
 */
data class Voucher(
    var id: UUID,
    var value: Int
)

/** List of the all the vouchers (initially empty). */
val listVouchers = arrayListOf<Voucher>()

/**
 * Adapter to bind [Voucher] objects to an [android.widget.ListView].
 * @param ctx context of the Fragment where the Adapter will be used.
 * @param listVouchers list of the vouchers that is going to be displayed.
 * @param function
 */
class VoucherAdapter(
    private val ctx: Context,
    private val listVouchers: ArrayList<Voucher>,
    function: () -> Unit,
): ArrayAdapter<Voucher>(ctx, R.layout.list_voucher, listVouchers) {

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: (ctx as Activity).layoutInflater.inflate(R.layout.list_voucher, parent, false)

        with(listVouchers[pos]) {
            row.findViewById<TextView>(R.id.tv_name).text = id.toString()
            row.findViewById<TextView>(R.id.tv_value).text = ctx.getString(R.string.product_value, value)
        }
        return row
    }

    suspend fun updateVouchers(activity: MainActivity2, uuid: String) : Boolean{
        var nonce: UUID? = null

        // get nonce challenge
        try {
            val result = JSONObject(actionChallengeVouchers(uuid.toString()))
            nonce = UUID.fromString(result.getString("nonce").toString())
        } catch (_: Exception) {
            return false
        }

        val entry = activity.fetchEntryRSA()

        val buffer = ByteBuffer.allocate(16).apply {
            putLong(nonce.mostSignificantBits)
            putLong(nonce.leastSignificantBits)
        }
        val encrypted = Cipher.getInstance(CRYPTO_RSA_ENC_ALGO).run {
            init(Cipher.ENCRYPT_MODE, getPrivateKey(entry))
            doFinal(buffer.array())
        }

        var vouchers: JSONArray? = null
        var discount: Double? = null
        try {
            val result = JSONObject(actionGetVouchers(uuid.toString(), encrypted))
            vouchers = result.getJSONArray("vouchers")
            discount = result.getDouble("discount")

        } catch (_: Exception) {
            return false
        }

        listVouchers.clear()
        vouchersDB.deleteAll()

        for (i in 0 until vouchers.length()) {
            val voucherObject = vouchers.getJSONObject(i)
            val voucherUuid = UUID.fromString(voucherObject.getString("uuid"))
            val voucherValue = 15
            val voucher = Voucher(voucherUuid, voucherValue)
            vouchersDB.insert(voucher)
            listVouchers.add(voucher)
        }
        discountDB.saveDiscount(discount.toFloat())
        notifyDataSetChanged()
        return true
    }
}
