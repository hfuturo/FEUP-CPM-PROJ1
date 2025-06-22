package com.example.client.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.client.logic.Product
import com.example.client.logic.listProducts
import com.example.client.logic.listProductsTime
import java.util.UUID

private const val DB_NAME = "products.db"
private const val DB_VERSION = 1

/**
 * Database to keep the information of the shopping list.
 */
class ProductsDB(ctx: Context): SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    private val tableProducts = "Products"
    private val keyId = "Id"
    private val colUuid = "Uuid"
    private val colName = "Name"
    private val colCategory = "Category"
    private val colSubCategory = "SubCategory"
    private val colPrice = "Price"

    override fun onCreate(db: SQLiteDatabase) {
        val sqlCreateTable = "CREATE TABLE $tableProducts(" +
                "$keyId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$colUuid TEXT, " +
                "$colName VARCHAR(100), " +
                "$colCategory VARCHAR(100), " +
                "$colSubCategory VARCHAR(100)," +
                "$colPrice REAL)"

        db.execSQL(sqlCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $tableProducts")
        onCreate(db)
    }

    /**
     * Inserts the new product in the database.
     */
    fun insert(product: Product): Long {
        val values = ContentValues().also {
            with (product) {
                it.put(colUuid, id.toString())
                it.put(colName, name)
                it.put(colCategory, category)
                it.put(colSubCategory, subCategory)
                it.put(colPrice, price)
            }
        }
        return writableDatabase.insert(tableProducts, null, values)
    }

    /**
     * Deletes an existing product from the database.
     */
    fun delete(uuid: UUID) {
        writableDatabase.delete(tableProducts, "$colUuid = ?", arrayOf(uuid.toString()))
    }

    /**
     * Deletes all products from the database.
     */
    fun deleteAll() {
        writableDatabase.delete(tableProducts, null, null)
    }

    /**
     * Gets all the products from the database.
     */
    fun getProducts() {
        val cursor = readableDatabase.rawQuery("SELECT * FROM  $tableProducts", null)
        listProducts.clear()
        listProductsTime.clear()
        while (cursor.moveToNext()) {
            val product = Product(UUID.randomUUID(),"","","", 0f)
            with (product) {
                id = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(colUuid)))
                name = cursor.getString(cursor.getColumnIndexOrThrow(colName))
                category = cursor.getString(cursor.getColumnIndexOrThrow(colCategory))
                subCategory = cursor.getString(cursor.getColumnIndexOrThrow(colSubCategory))
                price = cursor.getFloat(cursor.getColumnIndexOrThrow(colPrice))
            }
            listProducts.add(product)
            listProductsTime.add(product)
        }
        cursor.close()
    }
}