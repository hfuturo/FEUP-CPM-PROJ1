package com.example.client.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.data.TransactionDB
import com.example.client.dialog.ErrorDialogFragment
import com.example.client.logic.TransactionAdapter
import com.example.client.logic.listTransactions
import com.example.client.logic.transactionDB
import com.example.client.logic.userDB
import kotlinx.coroutines.launch

class TransactionsFragment: Fragment() {

    private lateinit var transactionsListView: ListView
    private lateinit var buttonUpdate: Button
    private lateinit var empty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        transactionDB = TransactionDB(requireActivity().applicationContext)
        buttonUpdate = view.findViewById(R.id.bottom_button_update)

        transactionsListView = view.findViewById(R.id.lv_transaction)
        empty = view.findViewById(R.id.empty2)

        val activity = requireActivity() as MainActivity2
        activity.toolbar.title = "Past Transactions"

        transactionsListView = view.findViewById(R.id.lv_transaction)
        val uuid = userDB.getColumnValue("Uuid")

        buttonUpdate.setOnClickListener {
            if (uuid != null) {
                lifecycleScope.launch {
                    if (!(transactionsListView.adapter as TransactionAdapter).updateTransactions(activity, uuid)) {
                        if (isAdded) return@launch
                        ErrorDialogFragment.newInstance("The server was not available. Try again!").show(parentFragmentManager, "error_popup")
                        return@launch
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        transactionDB.getTransactions()
        transactionsListView.run {
            emptyView = empty
            adapter = TransactionAdapter(requireContext(), listTransactions) {}
        }
        registerForContextMenu(transactionsListView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listTransactions.clear()
    }
}