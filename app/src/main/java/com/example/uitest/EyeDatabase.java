package com.example.uitest;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * @Description This database is used to store the state of the board before the user exits to ensure that the user can continue to play next time
 * @Reference The original code comes from https://www.youtube.com/watch?v=6ojV--thA5c, the code for storing and reading scores has been eliminated
 */
public class EyeDatabase extends SQLiteOpenHelper {

    public EyeDatabase(@Nullable Context context) {
        super(context, "EyeDatabase.SQLite", null, 1);
        try{
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("INSERT INTO EyeMode VALUES('row1','2','0','0','0')");
            database.execSQL("INSERT INTO EyeMode VALUES('row2','0','0','0','0')");
            database.execSQL("INSERT INTO EyeMode VALUES('row3','0','0','2','0')");
            database.execSQL("INSERT INTO EyeMode VALUES('row4','0','0','0','0')");
            database.close();
        }catch (Exception e)
        {
        }
    }
    public Cursor getData(String sql)
    {
        SQLiteDatabase database = getReadableDatabase();
        return  database.rawQuery(sql, null);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS EyeMode(row_id text primary key, col1 text, col2 text,col3 text,col4 text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void setProcess(int matrix[][])
    {
        SQLiteDatabase database = getWritableDatabase();
        database.execSQL("UPDATE EyeMode SET col1 ='" +matrix[1][1] +"' WHERE row_id='row1'");
        database.execSQL("UPDATE EyeMode SET col2 ='" +matrix[1][2] +"' WHERE row_id='row1'");
        database.execSQL("UPDATE EyeMode SET col3 ='" +matrix[1][3] +"' WHERE row_id='row1'");
        database.execSQL("UPDATE EyeMode SET col4 ='" +matrix[1][4] +"' WHERE row_id='row1'");

        database.execSQL("UPDATE EyeMode SET col1 ='" +matrix[2][1] +"' WHERE row_id='row2'");
        database.execSQL("UPDATE EyeMode SET col2 ='" +matrix[2][2] +"' WHERE row_id='row2'");
        database.execSQL("UPDATE EyeMode SET col3 ='" +matrix[2][3] +"' WHERE row_id='row2'");
        database.execSQL("UPDATE EyeMode SET col4 ='" +matrix[2][4] +"' WHERE row_id='row2'");

        database.execSQL("UPDATE EyeMode SET col1 ='" +matrix[3][1] +"' WHERE row_id='row3'");
        database.execSQL("UPDATE EyeMode SET col2 ='" +matrix[3][2] +"' WHERE row_id='row3'");
        database.execSQL("UPDATE EyeMode SET col3 ='" +matrix[3][3] +"' WHERE row_id='row3'");
        database.execSQL("UPDATE EyeMode SET col4 ='" +matrix[3][4] +"' WHERE row_id='row3'");

        database.execSQL("UPDATE EyeMode SET col1 ='" +matrix[4][1] +"' WHERE row_id='row4'");
        database.execSQL("UPDATE EyeMode SET col2 ='" +matrix[4][2] +"' WHERE row_id='row4'");
        database.execSQL("UPDATE EyeMode SET col3 ='" +matrix[4][3] +"' WHERE row_id='row4'");
        database.execSQL("UPDATE EyeMode SET col4 ='" +matrix[4][4] +"' WHERE row_id='row4'");

    }
    public int[][]  getProcess(Context context){
        int a[][]= new int[5][5];
        Cursor cursor = new EyeDatabase(context).getData("SELECT *FROM EyeMode");
        for(int i=1;i<5;i++)
        {
            cursor.moveToPosition(i-1);
            for(int j=1;j<5;j++) a[i][j] = Integer.valueOf(cursor.getString(j));
        }
        return a;
    }
}
