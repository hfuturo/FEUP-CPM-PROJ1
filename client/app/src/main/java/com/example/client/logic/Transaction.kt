package com.example.client.logic

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.data.TransactionDB
import com.example.client.dialog.ErrorDialogFragment
import com.example.client.utils.Crypto
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.Cipher

lateinit var transactionDB: TransactionDB

data class Transaction (
    val id: UUID,
    val price: Double,
    val date: String
)

val listTransactions = arrayListOf<Transaction>()

class TransactionAdapter(
    private val ctx: Context,
    private val listTransactions: ArrayList<Transaction>,
    function: () -> Unit
): ArrayAdapter<Transaction>(ctx, R.layout.list_transaction, listTransactions) {

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: (ctx as Activity).layoutInflater.inflate(R.layout.list_transaction, parent, false)

        with(listTransactions[pos]) {
            row.findViewById<TextView>(R.id.tv_date).text = date
            row.findViewById<TextView>(R.id.tv_price).text = ctx.getString(R.string.product_price, price)
        }

        return row
    }

    suspend fun updateTransactions(activity: MainActivity2, uuid: String): Boolean {
        var nonce: UUID?

        try {
            val result = JSONObject(actionChallengeTransactions(uuid.toString()))
            nonce = UUID.fromString(result.getString("nonce").toString())
        } catch (_: Exception) {
            return false
        }

        val entry = activity.fetchEntryRSA()

        val buffer = ByteBuffer.allocate(16).apply {
            putLong(nonce.mostSignificantBits)
            putLong(nonce.leastSignificantBits)
        }

        val encrypted = Cipher.getInstance(Crypto.CRYPTO_RSA_ENC_ALGO).run {
            init(Cipher.ENCRYPT_MODE, getPrivateKey(entry))
            doFinal(buffer.array())
        }

        var transactions: JSONArray? = null
        try {
            val result = JSONObject(actionGetTransactions(uuid.toString(), encrypted))
            Log.d("result", result.toString())
            transactions = result.getJSONArray("transactions")
            Log.d("transactions", transactions.toString())
        } catch (_: Exception) {
            return false
        }

        listTransactions.clear()
        transactionDB.deleteAll()

        for (i in 0 until transactions.length()) {
            val transactionObject = transactions.getJSONObject(i)
            val transactionUuid = UUID.fromString(transactionObject.getString("uuid"))
            val transactionPrice = transactionObject.getString("price").toDouble()
            val transactionDate = transactionObject.getString("date")
            val transaction = Transaction(transactionUuid, transactionPrice, transactionDate)
            transactionDB.insert(transaction)
            listTransactions.add(transaction)
        }

        listTransactions.reverse()

        notifyDataSetChanged()
        return true
    }
}