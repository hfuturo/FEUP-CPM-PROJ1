package com.example.client.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.client.R

/**
 * Fragment to display negative feedback, such as an error message.
 */
class ErrorDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fragment_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val errorTextView = view.findViewById<TextView>(R.id.error)
        val message = arguments?.getString("error_message") ?: "Error - Unknown"
        errorTextView.text = message
    }

    companion object {
        fun newInstance(message: String): ErrorDialogFragment {
            val fragment = ErrorDialogFragment()
            val args = Bundle()
            args.putString("error_message", message)
            fragment.arguments = args
            return fragment
        }
    }
}