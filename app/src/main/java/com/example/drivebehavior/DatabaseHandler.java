package com.example.drivebehavior;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

    public static final String DATABASE_NAME="driveBehavior_v1.db";
    public static final String TABLE_NAME="mytable";

    static final String COLUMN_NAME="name";
    static final String COLUMN_LAT="lat";
    static final String COLUMN_LONG="lon";
    static final String COLUMN_DATE="datetime";
    static final String COLUMN_SPEED="speed";
    static final String COLUMN_DISTANCE="distance";

    private static final int DATABASE_VERSION=1;

    public DatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d("CountRow1","  Create DB ");
    } // constructer

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " +
                TABLE_NAME +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_DATE +" TEXYT, "+ COLUMN_NAME + " TEXYT, "+ COLUMN_SPEED + " TEXYT, "+ COLUMN_DISTANCE + " TEXYT, "+ COLUMN_LAT + " TEXYT, "+COLUMN_LONG+ " TEXYT );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TABLE_NAME,"Upgrading "+oldVersion+" to "+newVersion);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public long addRecord(String name, String lat, String lon, String datetime,String speed, String distance){
        ContentValues values=new ContentValues();
        values.put(COLUMN_NAME,name);
        values.put(COLUMN_LAT, lat);
        values.put(COLUMN_LONG, lon);
        values.put(COLUMN_DATE, datetime);
        values.put(COLUMN_SPEED, speed);
        values.put(COLUMN_DISTANCE, distance);

        SQLiteDatabase db=this.getWritableDatabase();

        long row=db.insert(DatabaseHandler.TABLE_NAME, null, values);

        Log.d(TABLE_NAME,"inserted at row "+row+" "+name+" "+lat+" "+lon+ "  "+datetime+" "+speed+" "+distance);
        db.close();
        return row;
    } // addRecord

    public Cursor getAllRecord(){
        SQLiteDatabase db=this.getWritableDatabase();
        String[] columns={"_id","datetime","name","speed","distance","lat","lon"};
        Cursor cur=db.query(TABLE_NAME, columns, null, null, null ,null ,null );
        Log.d(TABLE_NAME," count "+cur.getCount());
        //db.close();
        return cur;
    } //Read all records

    public void deleteRecord(long recID){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TABLE_NAME, "_id=?", new String[] {String.valueOf(recID)} );
        db.close();
    } // del record

    public void clearDatabase(String TABLE_NAME) {
        SQLiteDatabase db = this.getReadableDatabase();
        String clearDBQuery = "DELETE FROM " + TABLE_NAME;
        db.execSQL(clearDBQuery);
    }

    public int getRecordCount(){
        String countQuery="SELECT _id FROM "+TABLE_NAME;
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cur=db.rawQuery(countQuery, null);
        //db.close();
        return cur.getCount();

    } // record count




}
