package com.example.client.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.data.DiscountDB
import com.example.client.data.VouchersDB
import com.example.client.logic.VoucherAdapter
import com.example.client.logic.discountDB
import com.example.client.logic.listVouchers
import com.example.client.logic.vouchersDB
import com.example.client.dialog.ErrorDialogFragment
import com.example.client.logic.userDB
import kotlinx.coroutines.launch

/**
 * Fragment used to show the user's vouchers.
 */
class VouchersFragment : Fragment() {

    private lateinit var discountText: TextView
    private lateinit var vouchersText : TextView
    private lateinit var vouchersListView: ListView
    private lateinit var empty: TextView
    private lateinit var buttonUpdate : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_vouchers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (this.requireActivity() as MainActivity2).toolbar.title = "Discounts & Vouchers"

        // Initialize the views
        discountText = view.findViewById<TextView>(R.id.valueDiscount)
        vouchersText = view.findViewById<TextView>(R.id.valueVouchers)
        vouchersListView = view.findViewById<ListView>(R.id.lv_voucher)
        empty = view.findViewById(R.id.empty2)
        buttonUpdate = view.findViewById<Button>(R.id.bottom_button_update)

        // Initialize the database
        vouchersDB = VouchersDB(requireActivity().applicationContext)
        discountDB = DiscountDB(requireActivity().applicationContext)

        val uuid = userDB.getColumnValue("Uuid")
        val activity = requireActivity() as MainActivity2

        buttonUpdate.setOnClickListener {
            if (uuid != null) {
                lifecycleScope.launch {
                    if (!(vouchersListView.adapter as VoucherAdapter).updateVouchers(activity, uuid)) {
                        if (!isAdded) return@launch
                        ErrorDialogFragment.newInstance("The server was not available. Try again!").show(parentFragmentManager, "error_popup")
                        return@launch
                    }
                    updateTotalVouchers()
                    updateTotalDiscount()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vouchersDB.getVouchers()
        vouchersListView.run {
            emptyView = empty
            adapter = VoucherAdapter(requireContext(), listVouchers) {
            }
        }
        registerForContextMenu(vouchersListView)
        updateTotalVouchers()
        updateTotalDiscount()
    }

    private fun updateTotalDiscount(){
        discountText.text = getString(R.string.price_format, discountDB.getDiscount())
    }

    private fun updateTotalVouchers(){
        vouchersText.text = listVouchers.size.toString()
    }
}