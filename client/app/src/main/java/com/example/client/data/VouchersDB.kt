package com.example.client.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.client.logic.Voucher
import com.example.client.logic.listVouchers
import java.util.UUID

private const val DB_NAME = "vouchers.db"
private const val DB_VERSION = 1

/**
 * Database to keep the information of the vouchers.
 */
class VouchersDB(ctx: Context): SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    private val tableVouchers = "Vouchers"
    private val keyId = "Id"
    private val colUuid = "Uuid"
    private val colValue = "Value"

    override fun onCreate(db: SQLiteDatabase) {
        val sqlCreateTable = "CREATE TABLE $tableVouchers(" +
                "$keyId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$colUuid TEXT, " +
                "$colValue INTEGER)"
        db.execSQL(sqlCreateTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $tableVouchers")
        onCreate(db)
    }

    /**
     * Inserts the new voucher in the database.
     */
    fun insert(voucher: Voucher): Long {
        val values = ContentValues().also {
            with (voucher) {
                it.put(colUuid, id.toString())
                it.put(colValue, value)
            }
        }
        return writableDatabase.insert(tableVouchers, null, values)
    }

    /**
     * Deletes all vouchers from the database.
     */
    fun deleteAll() {
        writableDatabase.delete(tableVouchers, null, null)
    }

    /**
     * Gets all the vouchers from the database.
     */
    fun getVouchers() {
        val cursor = readableDatabase.rawQuery("SELECT * FROM  $tableVouchers", null)
        listVouchers.clear()
        while (cursor.moveToNext()) {
            val voucher = Voucher(UUID.randomUUID(),0)
            with (voucher) {
                id = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(colUuid)))
                value = cursor.getInt(cursor.getColumnIndexOrThrow(colValue))
            }
            listVouchers.add(voucher)
        }
        cursor.close()
    }
}