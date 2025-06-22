package com.example.client.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import com.example.client.MainActivity2
import com.example.client.R
import com.example.client.data.DiscountDB
import com.example.client.data.ProductsDB
import com.example.client.dialog.CheckoutDialogFragment
import com.example.client.logic.CategoryProduct
import com.example.client.logic.OrderProduct
import com.example.client.logic.ProductAdapter
import com.example.client.logic.listProducts
import com.example.client.logic.productsDB
import com.example.client.logic.productsDecodeMessage
import com.example.client.logic.userDB
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.nio.charset.StandardCharsets

/**
 * Fragment to display the shopping cart, allowing viewing and adding products from a QR Code.
 * The cart displays a list of products and has the ability to scan a QR Code to add a new product to the cart.
 * The QR Code contains encrypted information about the product that will be decoded and added to the list.
 */
class CartFragment : Fragment() {

    private lateinit var productListView: ListView
    private lateinit var empty: TextView
    private lateinit var totalTextView: TextView

    private lateinit var searchView: SearchView
    private lateinit var spinnerOrder: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var btQR: Button
    private lateinit var btEnd: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (this.requireActivity() as MainActivity2).toolbar.title = "Cart"


        // Configuration of the ListView to display the products
        productsDB = ProductsDB(requireActivity().applicationContext)
        productListView = view.findViewById<ListView>(R.id.lv_items)
        empty = view.findViewById(R.id.empty)
        totalTextView = view.findViewById(R.id.tv_total_value)

        // Configuration of buttons
        searchView = view.findViewById<SearchView>(R.id.searchView)
        spinnerOrder = view.findViewById<Spinner>(R.id.order_spinner)
        spinnerCategory = view.findViewById<Spinner>(R.id.category_spinner)
        btQR = view.findViewById<Button>(R.id.bottom_button_qr)
        btEnd = view.findViewById<Button>(R.id.bottom_button_end)

        configuratorProductsListView()
        configuratorBottomButtons()
        configuratorFilter()
    }

    private fun configuratorProductsListView(){
        productsDB.getProducts()
        productListView.run {
            emptyView = empty
            adapter = ProductAdapter(requireContext(), listProducts) { updateTotal() }
        }
        updateTotal()
        registerForContextMenu(productListView)
    }

    private fun configuratorBottomButtons() {
        btQR.setOnClickListener {
            scanQRCode()
        }
        btEnd.setOnClickListener {
            val checkoutDialog = CheckoutDialogFragment(listProducts.sumOf{ it.price.toDouble() })
            checkoutDialog.show(parentFragmentManager, "checkout_dialog")
        }
    }

    private fun configuratorFilter() {

        // set up category spinner
        spinnerCategory.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            CategoryProduct.entries.map { it.name.replace("_", " ").lowercase().replaceFirstChar(Char::uppercaseChar) }
        )
        spinnerCategory.setSelection(0)
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                (productListView.adapter as ProductAdapter).setCategoryProduct(CategoryProduct.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set up ordering spinner
        spinnerOrder.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            OrderProduct.entries.map {
                when (it.name) {
                    "ASCENDING_NAME" -> "Name (A-Z)"
                    "DESCENDING_NAME" -> "Name (Z-A)"
                    "ASCENDING_PRICE" -> "Price (Low to High)"
                    "DESCENDING_PRICE" -> "Price (High to Low)"
                    "ASCENDING_TIME" -> "Time (First to Last)"
                    "DESCENDING_TIME" -> "Time (Last to First)"
                    else -> ""
                }
            }
        )
        spinnerOrder.setSelection(0)
        spinnerOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                (productListView.adapter as ProductAdapter).setOrderProduct(OrderProduct.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set up search view
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.lowercase() ?: ""
                (productListView.adapter as ProductAdapter).setQueryProduct(query)
                return true
            }
        })
    }

    /**
     * Sums the price of all products.
     */
    private fun updateTotal(){
        val totalValue = listProducts.sumOf{ it.price.toDouble() }
        totalTextView.text = getString(R.string.price_format, totalValue)
    }

    /**
     * Starts the process of scanning a QR Code.
     */
    private fun scanQRCode() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        scanCodeLauncher.launch(options)
    }

    /**
     * Initializes the scanner launch and processes the QR Code result.
     */
    private val scanCodeLauncher = registerForActivityResult(ScanContract()) {
        if (it != null && it.contents != null) {
            val result = it.contents
            decodeAndShow(result.toByteArray(StandardCharsets.ISO_8859_1))
        }
    }

    /**
     * Decodes the encrypted QR Code tag and displays the new product in the list.
     * @param combined Encrypted tag obtained from the QR Code.
     */
    private fun decodeAndShow(combined: ByteArray) {
        val keyString: String? = userDB.getColumnValue("Key")

        if (keyString != null) {
            val product = productsDecodeMessage(combined, keyString)
            if (product != null) {
                (productListView.adapter as ProductAdapter).productsUpdateList(product)
                updateTotal()
            }
        }
    }
}