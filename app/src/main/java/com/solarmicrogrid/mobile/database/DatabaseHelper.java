package com.solarmicrogrid.mobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.solarmicrogrid.mobile.models.Reservation;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Local Database Helper for offline caching and session persistence.
 * Satisfies Assignment Requirement: Pure native Android with SQLite persistence.
 * Author: Member 4
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SolarMicrogridLocal.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_RESERVATIONS = "local_reservations";
    public static final String TABLE_USER_SESSION = "user_session";

    // Reservation Columns
    public static final String COL_ID = "id";
    public static final String COL_PROSUMER_ID = "prosumer_id";
    public static final String COL_NODE_ID = "node_id";
    public static final String COL_ENERGY_KWH = "energy_kwh";
    public static final String COL_START_TIME = "start_time";
    public static final String COL_END_TIME = "end_time";
    public static final String COL_STATUS = "status";
    public static final String COL_QR_PAYLOAD = "qr_payload";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createReservationTable = "CREATE TABLE " + TABLE_RESERVATIONS + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_PROSUMER_ID + " TEXT, "
                + COL_NODE_ID + " TEXT, "
                + COL_ENERGY_KWH + " REAL, "
                + COL_START_TIME + " TEXT, "
                + COL_END_TIME + " TEXT, "
                + COL_STATUS + " TEXT, "
                + COL_QR_PAYLOAD + " TEXT" + ");";

        String createUserTable = "CREATE TABLE " + TABLE_USER_SESSION + " ("
                + "nic TEXT PRIMARY KEY, "
                + "name TEXT, "
                + "role TEXT" + ");";

        db.execSQL(createReservationTable);
        db.execSQL(createUserTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESERVATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_SESSION);
        onCreate(db);
    }

    // Save or update reservation in SQLite
    public void saveReservation(Reservation res) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ID, res.getId());
        values.put(COL_PROSUMER_ID, res.getProsumerNic());
        values.put(COL_NODE_ID, res.getNodeId());
        values.put(COL_ENERGY_KWH, res.getReservedEnergyKwh());
        values.put(COL_START_TIME, res.getStartTime());
        values.put(COL_END_TIME, res.getEndTime());
        values.put(COL_STATUS, res.getStatus());
        values.put(COL_QR_PAYLOAD, res.getQrCodePayload());

        db.insertWithOnConflict(TABLE_RESERVATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Cache list of reservations
    public void saveAllReservations(List<Reservation> list) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Reservation res : list) {
                saveReservation(res);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Retrieve cached reservations
    public List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RESERVATIONS + " ORDER BY " + COL_START_TIME + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Reservation res = new Reservation();
                res.setId(cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)));
                res.setProsumerId(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROSUMER_ID)));
                res.setProsumerNic(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROSUMER_ID)));
                res.setNodeId(cursor.getString(cursor.getColumnIndexOrThrow(COL_NODE_ID)));
                res.setReservedEnergyKwh(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ENERGY_KWH)));
                res.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_START_TIME)));
                res.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_END_TIME)));
                res.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
                res.setQrCodePayload(cursor.getString(cursor.getColumnIndexOrThrow(COL_QR_PAYLOAD)));
                list.add(res);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Update reservation status in SQLite
    public void updateReservationStatus(String id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        if ("Cancelled".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
            values.put(COL_QR_PAYLOAD, "");
        }
        db.update(TABLE_RESERVATIONS, values, COL_ID + " = ?", new String[]{id});
    }

    // Retrieve a single cached reservation by ID
    public Reservation getReservationById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RESERVATIONS + " WHERE " + COL_ID + " = ?", new String[]{id});
        Reservation res = null;
        if (cursor.moveToFirst()) {
            res = new Reservation();
            res.setId(cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)));
            res.setProsumerId(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROSUMER_ID)));
            res.setProsumerNic(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROSUMER_ID)));
            res.setNodeId(cursor.getString(cursor.getColumnIndexOrThrow(COL_NODE_ID)));
            res.setReservedEnergyKwh(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ENERGY_KWH)));
            res.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_START_TIME)));
            res.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_END_TIME)));
            res.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
            res.setQrCodePayload(cursor.getString(cursor.getColumnIndexOrThrow(COL_QR_PAYLOAD)));
        }
        cursor.close();
        return res;
    }
}
