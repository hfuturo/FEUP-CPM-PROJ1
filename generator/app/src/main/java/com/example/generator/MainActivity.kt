package com.example.generator

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.generator.utils.Crypto.CRYPTO_ANDROID_KEYSTORE
import com.example.generator.utils.Crypto.CRYPTO_NAME
import com.example.generator.Grocery.Companion.parseGroceries
import com.example.generator.utils.configuratorToolbarTitle
import com.example.generator.utils.dpToPx
import com.example.generator.utils.isDarkThemeOn
import com.example.generator.utils.setInsetsPadding
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

var useDarkTheme = false

/**
 * Main activity responsible for generating cryptographic keys and encrypting data.
 * It manages key generation, user input, and encryption of tags to be sent to a second activity.
 */
class MainActivity : AppCompatActivity() {

    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val searchField by lazy { findViewById<SearchView>(R.id.searchField) }
    private val categorySpinner by lazy { findViewById<Spinner>(R.id.categorySpinner) }
    private val sortSpinner by lazy { findViewById<Spinner>(R.id.sortSpinner) }

    private lateinit var adapter: GroceryAdapter

    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    var entry: KeyStore.PrivateKeyEntry? = null
        get() {
            if (field == null)
                field = KeyStore.getInstance(CRYPTO_ANDROID_KEYSTORE).run {
                    load(null)
                    getEntry(CRYPTO_NAME, null) as KeyStore.PrivateKeyEntry?
                }
            return field
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("darkMode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setInsetsPadding(toolbar, top = dpToPx(-8f))
        configuratorToolbarTitle(this, toolbar)

        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All", "Fruit", "Vegetables", "Packages", "Dessert")
        )
        sortSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("None", "Name (A-Z)", "Name (Z-A)", "Price (Low to High)", "Price (High to Low)")
        )

        if (entry == null) generateKeys()
        publicKey = getPublicKey(entry)
        privateKey = getPrivateKey(entry)

        lifecycleScope.launch {
            informServer(publicKey)
            val groceries = JSONObject(getGroceries())
            setupRecyclerView(groceries)
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        val item = menu?.findItem(R.id.item_switch)
        val actionView = item?.actionView
        if (actionView != null) {
            var switch = actionView.findViewById<Switch>(R.id.itemSwitch)
            switch.isChecked = isDarkThemeOn()
            useDarkTheme = switch.isChecked
            setThemeMode(switch)
            switch.setOnClickListener {
                useDarkTheme = switch.isChecked
                saveThemePreference(useDarkTheme)
                setThemeMode(switch)
            }
        }
        return true
    }

    /**
     * Sets up the RecyclerView with the provided groceries data.
     *
     * @param groceries The groceries data to display in the RecyclerView.
     */
    private fun setupRecyclerView(groceries: JSONObject) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GroceryAdapter(parseGroceries(groceries), this)
        recyclerView.adapter = adapter

        setupFilters()
    }

    /**
     * Sets up filtering functionality.
     */
    private fun setupFilters() {
        val applyFilters = {
            val search = searchField.query.toString()
            val category = categorySpinner.selectedItem.toString()
            val sort = sortSpinner.selectedItem.toString()
            adapter.applyFilter(search, category, sort)
        }

        searchField.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters()
                return true
            }
        })


        categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        sortSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun setThemeMode(@SuppressLint("UseSwitchCompatOrMaterialCode") switch: Switch) {
        // define color and theme
        val drawableRes = if (useDarkTheme) R.drawable.baseline_dark_mode_24 else R.drawable.baseline_light_mode_24
        val nightMode = if (useDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        // apply
        val thumb = AppCompatResources.getDrawable(this, drawableRes)
        val tintColor = if (useDarkTheme) R.color.white else R.color.black

        thumb?.setTint(ContextCompat.getColor(this, tintColor))
        switch.thumbDrawable = thumb
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun saveThemePreference(isDarkMode: Boolean) {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        prefs.edit { putBoolean("darkMode", isDarkMode) }
    }
}