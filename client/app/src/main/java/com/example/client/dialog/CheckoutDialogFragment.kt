package com.example.client.dialog

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.client.MainActivity2
import com.example.client.MainActivity3
import com.example.client.MainActivity4
import com.example.client.R
import com.example.client.logic.actionChallengeVouchers
import com.example.client.logic.actionGetVouchers
import com.example.client.logic.listProducts
import com.example.client.logic.getPrivateKey
import com.example.client.logic.userDB
import com.example.client.utils.Crypto
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.Signature
import java.util.UUID
import javax.crypto.Cipher

class CheckoutDialogFragment(private val total: Double) : DialogFragment() {

    private lateinit var activity: MainActivity2
    private lateinit var spinnerTypePay: Spinner
    private lateinit var spinnerDiscount: Spinner
    private lateinit var spinnerVoucher: Spinner
    private lateinit var checkoutTotalText: TextView
    private lateinit var checkoutTotal: TextView
    private lateinit var checkoutDiscountRow: LinearLayout
    private lateinit var checkoutDiscount: TextView
    private lateinit var checkoutVoucherRow: LinearLayout
    private lateinit var checkoutVoucher: TextView
    private lateinit var checkoutNewTotalRow: LinearLayout
    private lateinit var checkoutNewTotal: TextView
    private lateinit var btnCheckout: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fragment_checkout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        spinnerTypePay = view.findViewById(R.id.spinner_type_pay)
        spinnerDiscount = view.findViewById(R.id.spinner_discount)
        spinnerVoucher = view.findViewById(R.id.spinner_voucher)
        checkoutTotalText = view.findViewById(R.id.checkout_total_text)
        checkoutTotal = view.findViewById(R.id.checkout_total)
        checkoutDiscountRow = view.findViewById(R.id.checkout_discount_row)
        checkoutDiscount = view.findViewById(R.id.checkout_discount)
        checkoutVoucherRow = view.findViewById(R.id.checkout_voucher_row)
        checkoutVoucher = view.findViewById(R.id.checkout_voucher)
        checkoutNewTotalRow = view.findViewById(R.id.checkout_new_total_row)
        checkoutNewTotal = view.findViewById(R.id.checkout_new_total)
        btnCheckout = view.findViewById(R.id.bt_checkout)

        val uuid = userDB.getColumnValue("Uuid")
        activity = requireActivity() as MainActivity2

        lifecycleScope.launch {

            // Get nonce challenge
            var nonce: UUID? = null
            try {
                val result = JSONObject(actionChallengeVouchers(uuid.toString()))
                nonce = UUID.fromString(result.getString("nonce"))
            } catch (_ : Exception) {
                if (!isAdded) {
                    return@launch
                }
                dismiss()
                ErrorDialogFragment.Companion.newInstance("The server was not available. Try again!").show(parentFragmentManager, "error_popup")
                return@launch
            }

            // Encrypt nonce
            val entry = activity.fetchEntryRSA()
            val buffer = ByteBuffer.allocate(16).apply {
                putLong(nonce.mostSignificantBits)
                putLong(nonce.leastSignificantBits)
            }
            val encrypted = Cipher.getInstance(Crypto.CRYPTO_RSA_ENC_ALGO).run {
                init(Cipher.ENCRYPT_MODE, getPrivateKey(entry))
                doFinal(buffer.array())
            }

            // Get and display the vouchers
            var vouchers: JSONArray? = null
            var discount: Double? = null
            try {
                val result = JSONObject(actionGetVouchers(uuid.toString(), encrypted))
                vouchers = result.getJSONArray("vouchers")
                discount = result.getDouble("discount")
            } catch (_ : Exception) {
                if (!isAdded) return@launch
                dismiss()
                ErrorDialogFragment.Companion.newInstance("The server was not available. Try again!").show(parentFragmentManager, "error_popup")
                return@launch
            }

            val optionsVoucher = arrayListOf<String>("None")
            for (i in 0 until vouchers.length()) {
                val voucher = vouchers.getJSONObject(i)
                optionsVoucher.add(voucher.getString("uuid"))
            }

            var uiDiscount = discount
            var uiNewTotal = total

            if (total > discount) {
                uiNewTotal -= uiDiscount
            } else {
                uiDiscount = total
                uiNewTotal = 0.0
            }

            checkoutTotal.text = getString(R.string.price_format, total)
            checkoutDiscount.text = getString(R.string.discount_format, uiDiscount)
            checkoutNewTotal.text = getString(R.string.price_format, uiNewTotal)

            spinnerDiscount.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selected = parent.getItemAtPosition(position).toString()
                    if (selected == "Yes") {
                        checkoutTotalText.setTypeface(null, Typeface.NORMAL)
                        checkoutTotalText.textSize = 16f
                        checkoutTotal.setTypeface(null, Typeface.NORMAL)
                        checkoutTotal.textSize = 16f
                        checkoutDiscountRow.visibility = View.VISIBLE
                        checkoutNewTotalRow.visibility = View.VISIBLE
                        checkoutVoucher.text = getString(R.string.price_format, (uiNewTotal * 0.15))
                    } else {
                        checkoutTotalText.setTypeface(null, Typeface.BOLD)
                        checkoutTotalText.textSize = 18f
                        checkoutTotal.setTypeface(null, Typeface.BOLD)
                        checkoutTotal.textSize = 18f
                        checkoutDiscountRow.visibility = View.GONE
                        checkoutNewTotalRow.visibility = View.GONE
                        checkoutVoucher.text = getString(R.string.price_format, (total * 0.15))
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            spinnerVoucher.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selected = parent.getItemAtPosition(position).toString()
                    if (selected == "None") {
                        checkoutVoucherRow.visibility = View.GONE
                    } else {
                        checkoutVoucherRow.visibility = View.VISIBLE
                        if (spinnerDiscount.selectedItem.toString() == "Yes") {
                            checkoutVoucher.text = getString(R.string.price_format, (uiNewTotal * 0.15))
                        }
                        else {
                            checkoutVoucher.text = getString(R.string.price_format, (total * 0.15))
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            setUpSpinner(listOf("QR-Code", "NFC"), spinnerTypePay)
            setUpSpinner(listOf("No", "Yes"), spinnerDiscount)
            setUpSpinner(optionsVoucher, spinnerVoucher)
            setUpCheckout()
        }
    }

    /**
     * Sets up the spinner with its options.
     * @param options Options available in the spinner.
     * @param spinner Spinner widget.
     */
    private fun setUpSpinner(options: List<String>, spinner: Spinner) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    /**
     * Sets up the checkout button.
     */
    private fun setUpCheckout() {
        btnCheckout.setOnClickListener {

            val typePay = spinnerTypePay.selectedItem.toString()
            val discount = spinnerDiscount.selectedItem.toString()
            val voucher = spinnerVoucher.selectedItem.toString()

            var activityType: Class<out Activity> = MainActivity3()::class.java
            var useDiscount = false
            var voucherId: UUID? = null

            if (typePay == "NFC") activityType = MainActivity4()::class.java
            if (discount == "Yes") useDiscount = true
            if (voucher != "None") voucherId = UUID.fromString(voucher)

            redirectCheckout(activityType, voucherId, useDiscount)
        }
    }

    /**
     * Change the activity according with the payment method
     * @param activityType Activity type to change to.
     * @param voucherId Id of the voucher selected (can be null).
     * @param useDiscount Boolean that corresponds to the use of discount.
     */
    private fun redirectCheckout(activityType: Class<out Activity>, voucherId: UUID?, useDiscount: Boolean) {

        val uuid = userDB.getColumnValue("Uuid")
        val products: List<Pair<UUID, Float>> = listProducts.map { product ->
            product.id to product.price
        }

        if (uuid != null && products.isNotEmpty()) {
            val encryptedTag = messageCheckout(UUID.fromString(uuid), products, voucherId, useDiscount)
            startActivity(Intent(requireActivity(), activityType).apply {
                putExtra("data", encryptedTag)
            })
        }
    }

    /**
     * Generates a checkout message encoded as a ByteArray for payment processing.
     * @param userId The UUID representing the user's ID.
     * @param products A list of pairs, each containing a product's UUID and its price (in cents).
     * @param voucherId An optional UUID representing the voucher ID (if available).
     * @param useDiscount A boolean flag indicating whether a discount is applied.
     * @return ByteArray representing the generated checkout message. Returns `null` if there is an error during generation.
     */
    private fun messageCheckout(userId: UUID, products: List<Pair<UUID, Float>>, voucherId: UUID?, useDiscount: Boolean): ByteArray? {
        try {
            val limitedProducts = products.take(10)
            val dataLen = 16 + 1 + limitedProducts.size * (16 + 4) + 1 + 1 + if (voucherId != null) 16 else 0
            val message = ByteArray(dataLen)

            ByteBuffer.wrap(message, 0, dataLen).apply {
                putLong(userId.mostSignificantBits)
                putLong(userId.leastSignificantBits)
                put(limitedProducts.size.toByte())
                for ((id, price) in limitedProducts) {
                    putLong(id.mostSignificantBits)
                    putLong(id.leastSignificantBits)
                    putFloat(price)
                }
                put(if (useDiscount) 1 else 0)
                put(if (voucherId != null) 1 else 0)
                voucherId?.let {
                    putLong(it.mostSignificantBits)
                    putLong(it.leastSignificantBits)
                }
            }

            val activity = requireActivity() as MainActivity2
            val entry = activity.fetchEntryEC()

            val signature = Signature.getInstance(Crypto.CRYPTO_EC_SIGN_ALGO).run {
                initSign(getPrivateKey(entry))
                update(message)
                sign()
            }

            val sigLen = signature.size
            val finalMessage = ByteArray(dataLen + sigLen)
            System.arraycopy(message, 0, finalMessage, 0, dataLen)
            System.arraycopy(signature, 0, finalMessage, dataLen, sigLen)
            return finalMessage

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}