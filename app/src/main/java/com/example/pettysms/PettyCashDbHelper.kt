package com.example.pettysms

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PettyCashDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "PettySms.db"
        const val TABLE_TRUCKS = "trucks"
        const val COL_ID = "id"
        const val COL_TRUCK_NO = "truck_no"
        const val COL_TRUCK_MAKE = "make"
        const val COL_TRUCK_OWNER = "owner"
        const val COL_TRUCK_ACTIVE_STATUS = "active_status"

        const val TABLE_OWNERS = "owners"
        const val COL_OWNER_ID = "id"
        const val COL_OWNER_NAME = "name"
        const val COL_OWNER_CODE = "owner_code"



        // Define your table schema
        private const val SQL_CREATE_TABLE_TRUCKS = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRUCKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRUCK_NO TEXT, 
                $COL_TRUCK_MAKE TETX,
                $COL_TRUCK_OWNER TEXT,
                $COL_TRUCK_ACTIVE_STATUS INTEGER
                
            )
        """

        private const val SQL_CREATE_TABLE_OWNERS = """
            CREATE TABLE IF NOT EXISTS $TABLE_OWNERS (
                $COL_OWNER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OWNER_NAME TEXT, 
                $COL_OWNER_CODE TEXT
            )
        """

    }

    init {
        // This code will run whenever a PettyCashDbHelper object is created
        val db = writableDatabase
        db.execSQL(SQL_CREATE_TABLE_OWNERS)
        db.execSQL(SQL_CREATE_TABLE_TRUCKS)
        db.close()
    }


    private fun insertTruck(truck: Truck) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TRUCK_NO, truck.truckNo)
            put(COL_TRUCK_MAKE, truck.make)
            put(COL_TRUCK_OWNER, truck.owner?.ownerCode)
            put(COL_TRUCK_ACTIVE_STATUS, if (truck.activeStatus == true) 1 else 0)

            // Add other truck details...
        }
        db.insert(TABLE_TRUCKS, null, values)
        db.close()
    }

    fun insertTrucks(trucks: List<Truck>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (truck in trucks) {
                insertTruck(truck)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }


    fun getLocalTrucks(): List<Truck> {
        val trucks = mutableListOf<Truck>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TRUCKS, arrayOf(COL_ID, COL_TRUCK_NO, COL_TRUCK_MAKE, COL_TRUCK_OWNER, COL_TRUCK_ACTIVE_STATUS), null, null, null, null, null)
        while (cursor.moveToNext()) {
            val truckNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_NO))
            val truckId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID))
            val truckMake = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_MAKE))
            val truckOwnerCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRUCK_OWNER))
            val truckActiveStatusInteger = cursor.getInt(cursor.getColumnIndexOrThrow(
                COL_TRUCK_ACTIVE_STATUS))
            var truckActiveStatus = true
            if(truckActiveStatusInteger == 0){
                truckActiveStatus = false
            }

            val owner = getOwnerByCode(truckOwnerCode) ?: Owner(1, "Abdulcon Enterprises Limited", "abdulcon")

            trucks.add(Truck(truckId, truckNo, truckMake, owner, truckActiveStatus))
        }
        cursor.close()
        db.close()
        return trucks
    }

    fun getOwnerByCode(ownerCode: String): Owner? {
        val db = readableDatabase
        val selection = "$COL_OWNER_CODE = ?"
        val selectionArgs = arrayOf(ownerCode)
        val cursor = db.query(TABLE_OWNERS, null, selection, selectionArgs, null, null, null)
        var owner: Owner? = null
        try {
            if (cursor.moveToFirst()) {
                val ownerId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_NAME))
                owner = Owner(ownerId, ownerName, ownerCode)
            }
        } finally {
            cursor.close()
            // Do not close the database connection here
        }
        // Close the database connection outside of the try-finally block
        db.close()
        return owner
    }



    fun insertOwner(owner: Owner){
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_OWNER_NAME, owner.name)
            put(COL_OWNER_CODE, owner.ownerCode)
            // Add other owner details...
        }
        db.insert(TABLE_OWNERS, null, values)
        db.close()
    }

    fun getAllOwners(): List<Owner> {
        val owners = mutableListOf<Owner>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OWNERS", null)
        cursor.use {
            while (it.moveToNext()) {
                val ownerId = it.getInt(it.getColumnIndexOrThrow(COL_OWNER_ID))
                val ownerName = it.getString(it.getColumnIndexOrThrow(COL_OWNER_NAME))
                val ownerCode = it.getString(it.getColumnIndexOrThrow(COL_OWNER_CODE))
                owners.add(Owner(ownerId, ownerName, ownerCode))
            }
        }
        // Close the database connection after the cursor is closed
        db.close()
        return owners
    }

    // Function to insert a list of owners into the local database
    fun insertOwners(owners: List<Owner>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            // Insert each owner into the owners table using insertOwner function
            for (owner in owners) {
                insertOwner(owner)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (db.isOpen) { // Check if the database connection is open
                db.endTransaction()
                db.close()
            }
        }
    }


    override fun onCreate(db: SQLiteDatabase) {
        println("Niko kwenye oncreate")
        db.execSQL(SQL_CREATE_TABLE_OWNERS)
        db.execSQL(SQL_CREATE_TABLE_TRUCKS)
        db.close()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades here
    }
}