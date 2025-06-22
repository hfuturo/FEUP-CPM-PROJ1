package com.example.client.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.client.logic.Transaction
import com.example.client.logic.listTransactions
import java.util.UUID

private const val DB_NAME = "transaction.db"
private const val DB_VERSION = 1

class TransactionDB(ctx: Context): SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    private val tableTransaction = "PastTransaction"
    private val keyID = "Id"
    private val colUuid = "Uuid"
    private val colPrice = "Price"
    private val colDate = "Date"

    override fun onCreate(db: SQLiteDatabase) {
        val sqlCreateTable = "CREATE TABLE $tableTransaction(" +
                "$keyID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$colUuid TEXT, " +
                "$colPrice REAL, " +
                "$colDate TEXT)"
        db.execSQL(sqlCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $tableTransaction")
        onCreate(db)
    }

    fun insert(transaction: Transaction): Long {
        val values = ContentValues().also {
            with (transaction) {
                it.put(colUuid, id.toString())
                it.put(colPrice, price)
                it.put(colDate, date)
            }
        }

        return writableDatabase.insert(tableTransaction, null, values)
    }

    fun deleteAll() {
        writableDatabase.delete(tableTransaction, null, null)
    }

    fun getTransactions() {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $tableTransaction", null)
        listTransactions.clear()
        while (cursor.moveToNext()) {
            val transaction = Transaction(
                UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(colUuid))),
                cursor.getDouble(cursor.getColumnIndexOrThrow(colPrice)),
                cursor.getString(cursor.getColumnIndexOrThrow(colDate))
            )
            listTransactions.add(transaction)
        }
        listTransactions.reverse()
        cursor.close()
    }
}