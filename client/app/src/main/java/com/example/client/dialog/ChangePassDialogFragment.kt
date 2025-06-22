package com.example.client.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.client.R
import com.example.client.logic.userDB

class ChangePassDialogFragment : DialogFragment() {

    var onPassChanged: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fragment_change_pass, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var changePassOldInput = view.findViewById<EditText>(R.id.changePassOldInput)
        var changePassNewInput = view.findViewById<EditText>(R.id.changePassNewInput)
        var changePassNewConfirmInput = view.findViewById<EditText>(R.id.changePassNewConfirmInput)

        var changePassButton = view.findViewById<Button>(R.id.changePassButton)

        changePassButton.setOnClickListener {
            val old = changePassOldInput.text.toString()
            val new = changePassNewInput.text.toString()
            val newConfirm = changePassNewConfirmInput.text.toString()

            if (old != userDB.getColumnValue("Pass")) {
                ErrorDialogFragment.Companion.newInstance("Your current password was wrong. Try again!").show(parentFragmentManager, "error_popup")
            }
            else if (new != newConfirm) {
                ErrorDialogFragment.Companion.newInstance("Your new password confirmation failed. Try again!").show(parentFragmentManager, "error_popup")
            }
            else {
                onPassChanged?.invoke(new)
                dismiss()
            }
        }
    }
}