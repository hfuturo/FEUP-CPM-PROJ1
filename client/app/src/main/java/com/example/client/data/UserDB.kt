package com.example.client.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "user.db"
private const val DB_VERSION = 1

/**
 * Database to keep the information of the user.
 */
class UserDB(ctx: Context): SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    private val tableUser = "User"
    private val keyId = "Id"
    private val colUuid = "Uuid"
    private val colName = "Name"
    private val colNick = "Nick"
    private val colPass = "Pass"
    private val colCardNumber = "CardNumber"
    private val colCardDate = "CardDate"
    private val colSelectedCardType = "SelectedCardType"
    private val colKey = "Key"

    override fun onCreate(db: SQLiteDatabase) {
        val sqlCreateTable = "CREATE TABLE $tableUser(" +
                "$keyId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$colUuid TEXT, " +
                "$colName TEXT, " +
                "$colNick TEXT, " +
                "$colPass TEXT, " +
                "$colCardNumber TEXT, " +
                "$colCardDate TEXT, " +
                "$colSelectedCardType TEXT, " +
                "$colKey Text)"
        db.execSQL(sqlCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $tableUser")
        onCreate(db)
    }

    /**
     * Saves user information after registration.
     */
    fun saveUser(uuid: String, name: String, nick: String, pass: String, cardNumber: String, cardDate: String, selectedCardType: String, key: String) : Long {
        val values = ContentValues().apply {
            put(keyId, 1)
            put(colUuid, uuid)
            put(colName, name)
            put(colNick, nick)
            put(colPass, pass)
            put(colCardNumber, cardNumber)
            put(colCardDate, cardDate)
            put(colSelectedCardType, selectedCardType)
            put(colKey, key)
        }
        return writableDatabase.insertWithOnConflict(tableUser, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Updates specific parameter of the user.
     * @param columnName Name of the column to be changed.
     * @param newValue New value for the column.
     */
    fun updateColumn(columnName: String, newValue: String): Int {
        val values = ContentValues().apply {
            put(columnName, newValue)
        }
        return writableDatabase.update(tableUser, values, "$keyId = ?", arrayOf("1"))
    }

    /**
     * Gets value from specific column.
     * @param columnName Name of the column where the value is.
     */
    fun getColumnValue(columnName: String): String? {
        val cursor = readableDatabase.query(
            tableUser,
            arrayOf(columnName),
            "$keyId = ?",
            arrayOf("1"),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(columnName))
            }
        }
        return null
    }

    /**
     * Checks if the database is empty or not (use to check if registration is needed).
     */
    fun isDatabaseEmpty(): Boolean {
        val cursor = readableDatabase.query(
            tableUser,
            arrayOf("COUNT(*)"),
            null,
            null,
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0) == 0
            }
        }

        return true
    }
}