package com.example.client.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "discount.db"
private const val DB_VERSION = 1

/**
 * Database to keep the information of the discount.
 */
class DiscountDB(ctx: Context): SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    private val tableDiscount = "Discount"
    private val keyId = "Id"
    private val colValue = "Value"

    override fun onCreate(db: SQLiteDatabase) {
        val sqlCreateTable = "CREATE TABLE $tableDiscount(" +
                "$keyId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$colValue REAL)"
        db.execSQL(sqlCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $tableDiscount")
        onCreate(db)
    }

    /**
     * Saves or updates the discount value (always uses id = 1).
     */
    fun saveDiscount(value: Float) : Long {
        val values = ContentValues().apply {
            put(keyId, 1)
            put(colValue, value)
        }
        return writableDatabase.insertWithOnConflict(tableDiscount, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Gets the current discount value (returns 0f if no value is stored).
     */
    fun getDiscount(): Float {
        val cursor = readableDatabase.rawQuery(
            "SELECT $colValue FROM $tableDiscount WHERE $keyId = 1", null
        )
        var value = 0f
        if (cursor.moveToFirst()) {
            value = cursor.getFloat(cursor.getColumnIndexOrThrow(colValue))
        }
        cursor.close()
        return value
    }
}