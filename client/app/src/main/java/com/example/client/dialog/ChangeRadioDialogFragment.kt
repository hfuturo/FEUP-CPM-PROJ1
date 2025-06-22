package com.example.client.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.example.client.R
import com.example.client.logic.userDB

class ChangeRadioDialogFragment : DialogFragment() {

    var onRadioChanged: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fragment_change_radio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupType)
        val visaRadio = view.findViewById<RadioButton>(R.id.visaButton)
        val masterclassRadio = view.findViewById<RadioButton>(R.id.mastercardButton)
        val discoverRadio = view.findViewById<RadioButton>(R.id.discoverButton)

        val selectedType = userDB.getColumnValue("SelectedCardType")

        if(selectedType == "Visa") radioGroup.check(visaRadio.id)
        else if(selectedType == "Masterclass") radioGroup.check(masterclassRadio.id)
        else if(selectedType == "Discover") radioGroup.check(discoverRadio.id)

        var changeTypeButton = view.findViewById<Button>(R.id.changeTypeButton)

        changeTypeButton.setOnClickListener {
            val selectedRadioButtonId = radioGroup.checkedRadioButtonId
            if (selectedRadioButtonId != -1) {
                val selectedRadioButton = view.findViewById<RadioButton>(selectedRadioButtonId)
                onRadioChanged?.invoke(selectedRadioButton.text.toString())
                dismiss()
            }
        }
    }
}