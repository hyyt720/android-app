package com.example.emojishow;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "ImageDatabase", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE images (id INTEGER PRIMARY KEY, filePath TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS images");
        onCreate(db);
    }

    public List<String> getImageFilePaths(int limit, int offset) {
        List<String> paths = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT filePath FROM images LIMIT ? OFFSET ?", new String[]{String.valueOf(limit), String.valueOf(offset)});
        while (cursor.moveToNext()) {
            paths.add(cursor.getString(0));
        }
        cursor.close();
        return paths;
    }
}
