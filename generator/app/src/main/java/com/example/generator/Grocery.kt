package com.example.generator

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isGone
import com.example.generator.utils.collapse
import com.example.generator.utils.expand
import com.example.generator.utils.isDarkThemeOn
import org.json.JSONObject
import java.util.UUID

/**
 * Data class representing a Grocery item.
 */
data class Grocery (
    val name: String,
    val category: String,
    val subCategory: String,
    val description: String,
    val imagePath: String,
    val price: Float
) {
    companion object {

        /**
         * Parses a JSON object containing a list of groceries.
         *
         * @param jsonObject The JSON object containing the groceries.
         *
         * @return A list of groceries.
         */
        fun parseGroceries(jsonObject: JSONObject): List<Grocery> {
            val groceryList = mutableListOf<Grocery>()
            val groceriesArray = jsonObject.getJSONArray("groceries")

            for (i in 0 until groceriesArray.length()) {
                val item = groceriesArray.getJSONObject(i)

                val grocery = Grocery(
                    name = item.getString("Name"),
                    category = item.getString("Category"),
                    subCategory = item.getString("SubCategory"),
                    description = item.getString("Description"),
                    price = item.getString("Price").toFloat(),
                    imagePath = item.getString("ImagePath")
                )

                groceryList.add(grocery)
            }
            return groceryList
        }
    }
}

/**
 * RecyclerView Adapter to display and filter the list of groceries.
 *
 * @property groceries The full list of groceries.
 * @property mainContext The [MainActivity] context used for resource access and navigation.
 */
class GroceryAdapter(private val groceries: List<Grocery>, private val mainContext: MainActivity):
    RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder>() {

    private var filteredList: List<Grocery> = groceries

    /**
     * ViewHolder for each grocery item in the RecyclerView.
     */
    class GroceryViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.groceryCategory)
        val image: ImageView = view.findViewById(R.id.groceryImage)
        val title: TextView = view.findViewById(R.id.groceryTitle)
        val arrow: ImageView = view.findViewById(R.id.toggleArrow)
        val price: TextView = view.findViewById(R.id.groceryPrice)
        val description: TextView = view.findViewById(R.id.groceryDescription)
        val cartButton: Button = view.findViewById(R.id.addToCartButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroceryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.grocery_item, parent, false)
        return GroceryViewHolder(view)
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        val grocery = filteredList[position]

        val resourceID = mainContext.resources.getIdentifier(
            grocery.imagePath,
            "drawable",
            mainContext.packageName
        )

        val arrowColor = if (mainContext.isDarkThemeOn()) {
            ContextCompat.getColor(mainContext, R.color.white)
        } else {
            ContextCompat.getColor(mainContext, R.color.black)
        }

        holder.arrow.setColorFilter(arrowColor)

        holder.arrow.setOnClickListener {
            if (holder.description.isGone) {
                expand(holder.description)
                holder.arrow.animate().rotation(-180f).setDuration(300).start()
            }
            else {
                collapse(holder.description)
                holder.arrow.animate().rotation(0f).setDuration(300).start()
            }
        }

        Log.d("Path", grocery.imagePath)

        holder.category.text = grocery.category
        holder.image.setImageResource(resourceID)
        holder.title.text = if (grocery.name == grocery.subCategory) { grocery.name } else { "${grocery.subCategory} (${grocery.name})" }
        holder.price.text = holder.itemView.context.getString(R.string.grocery_price, grocery.price.toString())
        holder.description.text = grocery.description

        holder.cartButton.setOnClickListener {
            val uuid = UUID.randomUUID()
            Log.d("UUID", uuid.toString())
            val encryptedTag = generateTag(mainContext.entry, uuid, grocery)
            Log.d("encryptedTag", encryptedTag.toString())
            mainContext.startActivity(Intent(mainContext, MainActivity2::class.java). apply {
                putExtra("data", encryptedTag)
            })
        }
    }

    override fun getItemCount() = filteredList.size

    /**
     * Applies filtering based on each filter option.
     *
     * @param searchText Text to search for in grocery name or subcategory
     * @param category The selected categoty filter.
     * @param sortOption The selected sorting criteria.
     */
    fun applyFilter(searchText: String, category: String, sortOption: String) {
        var tempList = groceries

        if (category != "All") {
            tempList = tempList.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (searchText.isNotBlank()) {
            tempList = tempList.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                it.subCategory.contains(searchText, ignoreCase = true)
            }
        }

        filteredList = when (sortOption) {
            "Name (A-Z)" -> tempList.sortedBy { it.subCategory }
            "Name (Z-A)" -> tempList.sortedByDescending { it.subCategory }
            "Price (Low to High)" -> tempList.sortedBy { it.price }
            "Price (High to Low)" -> tempList.sortedByDescending { it.price }
            else -> tempList
        }

        notifyDataSetChanged()
    }
}