package com.example.client.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.dialog.ChangePassDialogFragment
import com.example.client.dialog.ChangeRadioDialogFragment
import com.example.client.dialog.ChangeTextDialogFragment
import com.example.client.logic.userDB
import com.example.client.utils.isDarkThemeOn
import androidx.core.content.edit


class ProfileFragment : Fragment() {

    private var useDarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (this.requireActivity() as MainActivity2).toolbar.title = "Profile"

        val switch = view.findViewById<Switch>(R.id.itemSwitch)

        val textName = view.findViewById<TextView>(R.id.textName)
        val textNick = view.findViewById<TextView>(R.id.textNick)
        val textPass = view.findViewById<TextView>(R.id.textPass)
        val textType = view.findViewById<TextView>(R.id.textType)
        val textNumber = view.findViewById<TextView>(R.id.textNumber)
        val textDate = view.findViewById<TextView>(R.id.textDate)

        val editNameButton = view.findViewById<ImageButton>(R.id.editNameButton)
        val editNickButton = view.findViewById<ImageButton>(R.id.editNickButton)
        val editPassButton = view.findViewById<ImageButton>(R.id.editPassButton)
        val editTypeButton = view.findViewById<ImageButton>(R.id.editTypeButton)
        val editNumberButton = view.findViewById<ImageButton>(R.id.editNumberButton)
        val editDateButton = view.findViewById<ImageButton>(R.id.editDateButton)

        textName.text = userDB.getColumnValue("Name")
        textNick.text = userDB.getColumnValue("Nick")
        textPass.text = userDB.getColumnValue("Pass")
        textType.text = userDB.getColumnValue("SelectedCardType")
        textNumber.text = userDB.getColumnValue("CardNumber")
        textDate.text = userDB.getColumnValue("CardDate")

        Log.d("switch", switch.toString())
        switch.isChecked = requireContext().isDarkThemeOn()
        useDarkMode = switch.isChecked
        setThemeMode(switch)
        switch.setOnClickListener {
            useDarkMode = switch.isChecked
            saveThemePreference(useDarkMode)
            setThemeMode(switch)
        }

        editNameButton.setOnClickListener {
            var dialog = ChangeTextDialogFragment.newInstance("Name", "text")
            dialog.onTextChanged = { newText ->
                userDB.updateColumn("Name", newText)
                textName.text = newText
            }
            dialog.show(parentFragmentManager, "change_text_dialog")
        }

        editNickButton.setOnClickListener {
            var dialog = ChangeTextDialogFragment.newInstance("Nick", "text")
            dialog.onTextChanged = { newText ->
                userDB.updateColumn("Nick", newText)
                textNick.text = newText
            }
            dialog.show(parentFragmentManager, "change_text_dialog")
        }

        editPassButton.setOnClickListener {
            var dialog = ChangePassDialogFragment()
            dialog.onPassChanged = { newPass ->
                userDB.updateColumn("Pass", newPass)
                textPass.text = newPass
            }
            dialog.show(parentFragmentManager, "change_pass_dialog")
        }

        editTypeButton.setOnClickListener {
            var dialog = ChangeRadioDialogFragment()
            dialog.onRadioChanged = { newText ->
                userDB.updateColumn("SelectedCardType", newText)
                textType.text = newText
            }
            dialog.show(parentFragmentManager, "change_text_dialog")
        }


        editNumberButton.setOnClickListener {
            var dialog = ChangeTextDialogFragment.newInstance("CardNumber", "number")
            dialog.onTextChanged = { newText ->
                userDB.updateColumn("CardNumber", newText)
                textNumber.text = newText
            }
            dialog.show(parentFragmentManager, "change_text_dialog")
        }

        editDateButton.setOnClickListener {
            var dialog = ChangeTextDialogFragment.newInstance("CardDate", "date")
            dialog.onTextChanged = { newText ->
                userDB.updateColumn("CardDate", newText)
                textDate.text = newText
            }
            dialog.show(parentFragmentManager, "change_text_dialog")
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun setThemeMode(switch: Switch) {
        val drawableRes = if (useDarkMode) R.drawable.baseline_dark_mode_24 else R.drawable.baseline_light_mode_24
        val nightMode = if (useDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

        val thumb = AppCompatResources.getDrawable(requireContext(), drawableRes)

        // Set tint color based on mode
        val tintColor = if (useDarkMode) R.color.white else R.color.black
        thumb?.setTint(ContextCompat.getColor(requireContext(), tintColor))

        switch.thumbDrawable = thumb
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun saveThemePreference(isDarkMode: Boolean) {
        val prefs = requireContext().getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)
        prefs.edit { putBoolean("darkMode", isDarkMode) }
    }
}