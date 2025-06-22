package com.example.client.logic

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.example.client.R
import com.example.client.data.ProductsDB
import com.example.client.utils.Crypto.CRYPTO_RSA_ENC_ALGO
import com.example.client.utils.Crypto.CRYPTO_RSA_KEY_SIZE
import com.example.client.utils.Crypto.CRYPTO_RSA_SIGN_ALGO
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.Signature
import java.util.UUID
import javax.crypto.Cipher
import kotlin.collections.filter
import kotlin.text.equals

/**
 * Data class representing a product.
 * @property id Unique identifier of the product.
 * @property name Name of the product.
 * @property category Category of the product.
 * @property subCategory Sub category of the product.
 * @property price Price of the product.
 */
data class Product(
    var id: UUID,
    var name: String,
    var category: String,
    var subCategory: String,
    var price: Float
)

/** Enum class with the types of ordering. */
enum class OrderProduct { ASCENDING_TIME, DESCENDING_TIME, ASCENDING_NAME, DESCENDING_NAME, ASCENDING_PRICE, DESCENDING_PRICE }
/** Enum class with the types of categories. */
enum class CategoryProduct { ALL, FRUIT, VEGETABLES, PACKAGES, DESSERT }

/** Database with the products. */
lateinit var productsDB: ProductsDB

/** List of all the products in the cart (initially empty). */
var listProducts = arrayListOf<Product>()
/** List of all the products in the cart (initially empty), by date of acquisition. */
val listProductsTime = arrayListOf<Product>()

/**
 * Decodes message from the qr code.
 * @param message Message retrieved from the qr code.
 * @param key Key (in string format) of the market.
 */
fun productsDecodeMessage(message: ByteArray, key: String) : Product? {
    var clearTextTag = ByteArray(0)
    val numberBytes = CRYPTO_RSA_KEY_SIZE / 8
    val totalSize = numberBytes * 2

    if (message.size < totalSize) return null
    val encryptedTag = message.copyOfRange(0, numberBytes)
    val signature = message.copyOfRange(numberBytes, numberBytes + numberBytes)

    try {
        val key = base64ToPublicKey(key)
        clearTextTag = Cipher.getInstance(CRYPTO_RSA_ENC_ALGO).run {
            init(Cipher.DECRYPT_MODE, key)
            doFinal(encryptedTag)
        }
        val signatureVerifier = Signature.getInstance(CRYPTO_RSA_SIGN_ALGO).run {
            initVerify(key)
            update(encryptedTag)
            verify(signature)
        }
        if(!signatureVerifier) return null
    }
    catch (_: Exception) {
        return null
    }

    val tag = ByteBuffer.wrap(clearTextTag)
    val tagId = tag.int
    Log.d("tagID", tagId.toString())
    val id = UUID(tag.long, tag.long)

    val nameLength = tag.get().toInt()
    val nameBytes = ByteArray(nameLength)
    tag.get(nameBytes)

    val category = when (tag.get().toInt()) {
        1 -> "Fruit"
        2 -> "Vegetables"
        3 -> "Packages"
        4 -> "Dessert"
        else -> "Other"
    }

    val subCategoryLength = tag.get().toInt()
    val subCategoryBytes = ByteArray(subCategoryLength)
    tag.get(subCategoryBytes)

    val price = tag.getFloat()

    val subCategory = String(subCategoryBytes, StandardCharsets.ISO_8859_1)
    val name = if (String(nameBytes, StandardCharsets.ISO_8859_1) == "") { subCategory }
                else { String(nameBytes, StandardCharsets.ISO_8859_1) }
    return Product(id, name, category, subCategory, price)
}

/**
 * Adapter to bind [Product] objects to an [android.widget.ListView].
 * @param ctx context of the Fragment where the Adapter will be used.
 * @param listProducts list of the products that is going to be displayed and manipulated.
 * @param onRemove callback function to update total value.
 */
class ProductAdapter(
    private val ctx: Context,
    private val listProducts: ArrayList<Product>,
    private val onRemove: (Int) -> Unit
): ArrayAdapter<Product>(ctx, R.layout.list_item, listProducts) {

    private var currentOrderProduct = OrderProduct.ASCENDING_TIME
    private var currentCategoryProduct = CategoryProduct.ALL
    private var currentQueryProduct = ""

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: (ctx as Activity).layoutInflater.inflate(R.layout.list_item, parent, false)

        // fill with the products' data
        with(listProducts[pos]) {
            row.findViewById<TextView>(R.id.tv_name).text = if (name == subCategory) name else "$subCategory ($name)"
            row.findViewById<TextView>(R.id.tv_value).text = ctx.getString(R.string.price_format, price)
            Log.d("ID", id.toString())
            // define remove action
            row.findViewById<ImageButton>(R.id.bt_remove).setOnClickListener {
                productsDB.delete(listProducts[pos].id)
                listProductsTime.removeIf { it.id == listProducts[pos].id }
                listProducts.removeAt(pos)
                notifyDataSetChanged()
                onRemove(pos)
            }
        }
        return row
    }

    fun setCategoryProduct(type : CategoryProduct) {
        currentCategoryProduct = type
        productsFilter()
    }

    fun setOrderProduct(type: OrderProduct) {
        currentOrderProduct = type
        productsFilter()
    }
    
    fun setQueryProduct(query: String) {
        currentQueryProduct = query
        productsFilter()
    }

    /**
     * Sort the products in listProducts according to the order desired by the user.
     */
    private fun productsSort() {
        listProducts.clear()
        when (currentOrderProduct) {
            OrderProduct.ASCENDING_TIME -> {
                listProducts.addAll(listProductsTime)
            }
            OrderProduct.DESCENDING_TIME -> {
                listProducts.addAll(listProductsTime.reversed())
            }
            OrderProduct.ASCENDING_NAME -> {
                listProducts.addAll(listProductsTime)
                listProducts.sortBy { it.name.lowercase() }
            }
            OrderProduct.DESCENDING_NAME -> {
                listProducts.addAll(listProductsTime)
                listProducts.sortByDescending { it.name.lowercase() }
            }
            OrderProduct.ASCENDING_PRICE -> {
                listProducts.addAll(listProductsTime)
                listProducts.sortBy { it.price }
            }
            OrderProduct.DESCENDING_PRICE -> {
                listProducts.addAll(listProductsTime)
                listProducts.sortByDescending { it.price }
            }
        }
    }

    /**
     * Filters the products in listProducts according to their category.
     */
    private fun productsCategory() {
        if (currentCategoryProduct != CategoryProduct.ALL) {
            var filteredList = (listProducts.filter {
                it.category.equals(currentCategoryProduct.name.lowercase(), ignoreCase = true)
            }).toCollection(ArrayList())
            listProducts.clear()
            listProducts.addAll(filteredList)
        }
    }

    /**
     * Filters the products in listProducts according to their name.
     */
    private fun productsQuery() {
        if (currentQueryProduct != "") {
            var filteredList = listProducts.filter {
                it.name.lowercase().contains(currentQueryProduct) || it.category.lowercase().contains(currentQueryProduct)
            }.toCollection(ArrayList())
            listProducts.clear()
            listProducts.addAll(filteredList)
        }
    }

    /**
     * Perform all filters and sorting.
     */
    fun productsFilter() {
        productsSort()
        productsCategory()
        productsQuery()
        notifyDataSetChanged()
    }

    /**
     * Updates the list of products and database.
     * @param product Product to be added to the list.
     */
    fun productsUpdateList(product : Product) {
        productsDB.insert(product)
        listProductsTime.add(product)
        listProducts.add(product)
        productsFilter()
    }
}
