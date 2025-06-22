package com.example.client.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.client.R
import com.example.client.logic.userDB
import java.text.SimpleDateFormat
import kotlin.text.replace
import kotlin.text.split

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ChangeTextDialogFragment : DialogFragment() {

    private var param1: String? = null
    private var param2: String? = null

    var onTextChanged: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fragment_change_text, container, false)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var changeTextParameter = view.findViewById<TextView>(R.id.changeTextParameter)
        var changeTextInput = view.findViewById<EditText>(R.id.changeTextInput)
        var changeTextButton = view.findViewById<Button>(R.id.changeTextButton)

        val parameter: String? = userDB.getColumnValue(param1.toString())
        changeTextParameter.text = param1!!.uppercase()

        if(param2 == "text") changeTextInput.setRawInputType(InputType.TYPE_CLASS_TEXT)

        else if (param2 == "number") changeTextInput.setRawInputType(InputType.TYPE_CLASS_NUMBER)

        else if (param2 == "date") {
            changeTextInput.setRawInputType(InputType.TYPE_CLASS_NUMBER)
            changeTextInput.addTextChangedListener(object: TextWatcher {
                var isEditing = false
                var isDeleting = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isEditing || s == null) return
                    isDeleting = s.split("").count { it == "/" } != 1
                    isEditing = true

                    val digits = s.replace(Regex("\\D"), "")
                    val formatted = StringBuilder("__/__")
                    var lastDigitPos = 0

                    for (i in digits.indices) {
                        if (i < 4) {
                            val pos = if (i < 2) i else i+1
                            if (isDeleting) lastDigitPos = pos
                            formatted.setCharAt(pos, digits[i])
                        }
                    }

                    if (isDeleting) {
                        formatted.setCharAt(lastDigitPos, '_')
                        isDeleting = false
                    }

                    changeTextInput.setText(formatted)
                    val nextPos = formatted.indexOf('_').takeIf { it != -1 } ?: formatted.length
                    changeTextInput.setSelection(nextPos)
                    isEditing = false
                }
            })
        }

        changeTextInput.setText(parameter)
        changeTextButton.setOnClickListener {
            var newValue = changeTextInput.text.toString()
            if(newValue == "") {
                ErrorDialogFragment.Companion.newInstance("Nothing was written. Please correct it!").show(parentFragmentManager, "error_popup")
            }
            else {
                if (param2 == "date") {
                    val dateFormat = SimpleDateFormat("MM/yy")
                    dateFormat.isLenient = false
                    var newValueValid : String? = null
                    try {
                        newValueValid = dateFormat.format(dateFormat.parse(newValue))
                        onTextChanged?.invoke(newValueValid)
                        dismiss()
                    }
                    catch (_: Exception) {
                        ErrorDialogFragment.Companion.newInstance("The date is not valid. Please correct it!").show(parentFragmentManager, "error_popup")
                    }
                }
                else {
                    onTextChanged?.invoke(newValue)
                    dismiss()
                }
            }
        }
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        fun newInstance(param1: String, param2: String): ChangeTextDialogFragment {
            val fragment = ChangeTextDialogFragment()
            val args = Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
            }
            fragment.arguments = args
            return fragment
        }
    }
}