package com.example.client.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.client.MainActivity
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.dialog.ErrorDialogFragment
import com.example.client.logic.userDB
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment responsible to deal with the login authentication of the user.
 * Validates credentials provided against data saved, and if valid, redirects the user to the next Activity.
 * Also handles visual feedback to the user, showing snippets of progress or error.
 */
class LoginFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var button  = view.findViewById<Button>(R.id.login_button)

        button.setOnClickListener{

            // Capture data entered into text fields
            var name = view.findViewById<TextInputEditText>(R.id.input_name).text.toString()
            var nick = view.findViewById<TextInputEditText>(R.id.input_nick).text.toString()
            var pass = view.findViewById<TextInputEditText>(R.id.input_pass).text.toString()

            // Access data saved
            val realName = userDB.getColumnValue("Name")
            val realNick = userDB.getColumnValue("Nick")
            val realPass = userDB.getColumnValue("Pass")

            // If data entered is equal to data saved, then redirect to the next activity and close actual
            if(name == realName && nick == realNick && pass == realPass) {
                val activity = requireActivity() as MainActivity
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(activity, MainActivity2::class.java)
                        startActivity(intent)
                        activity.finish()
                    }
                }
            }

            // If the data does not match, then show error fragment
            else {
                ErrorDialogFragment.Companion.newInstance("The credentials are incorrect. Try again!").show(parentFragmentManager, "error_popup")
            }
        }
    }
}