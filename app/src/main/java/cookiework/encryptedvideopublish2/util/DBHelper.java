package cookiework.encryptedvideopublish2.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

import static cookiework.encryptedvideopublish2.Constants.*;

/**
 * Created by Administrator on 2017/01/16.
 */

public class DBHelper extends SQLiteOpenHelper {
    private static final String CREATE_PUBLISHER_STATEMENT =
            "create table publisher(username text unique, N text, d text)";
    private static final String SELECT_KEY_STATEENT =
            "select * from publisher where username=?";
    private static final String INSERT_KEY_STATEMENT =
            "insert into publisher(username, N, d) values(?,?,?)";
    private static final String DELETE_KEY_STATEMENT =
            "delete from publisher where username=?";
    private static final String UPDATE_KEY_STATEMENT =
            "update publisher set N=?, e=? where username=?";

    private static final String CREATE_VIDEOLOG_STATEMENT =
            "create table videolog(videoid int unique, tags text, key text)";
    private static final String ADD_VIDEOLOG_STATEMENT =
            "insert into videolog(videoid, tags, key) values(?,?,?)";
    private static final String DELETE_VIDEOLOG_STATEMENT =
            "delete from videolog where videoid=?";
    private static final String SELECT_VIDEOLOG_STATEMENT =
            "select * from videolog where videoid=?";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PUBLISHER_STATEMENT);
        db.execSQL(CREATE_VIDEOLOG_STATEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion == 1 && newVersion == 2){
            db.execSQL(CREATE_VIDEOLOG_STATEMENT);
        }
    }

    public String getN(String username){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(SELECT_KEY_STATEENT, new String[]{username});
        String result = null;
        if(cursor.moveToNext()){
            result = cursor.getString(cursor.getColumnIndex("N"));
        }
        cursor.close();
        db.close();
        return result;
    }

    public String getD(String username){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(SELECT_KEY_STATEENT, new String[]{username});
        String result = null;
        if(cursor.moveToNext()){
            result = cursor.getString(cursor.getColumnIndex("d"));
        }
        cursor.close();
        db.close();
        return result;
    }

    public boolean addKey(String username, String nString, String dString){
        if(getN(username) != null){
            return false;
        } else {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(INSERT_KEY_STATEMENT, new Object[]{username, nString, dString});
            db.close();
        }
        return true;
    }

    public void addVideoLog(int videoId, String tags, String key){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(DELETE_VIDEOLOG_STATEMENT, new Object[]{videoId});
        db.execSQL(ADD_VIDEOLOG_STATEMENT, new Object[]{videoId, tags, key});
        db.close();
    }

    public HashMap<String, String> getVideoLog(int videoId){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(SELECT_VIDEOLOG_STATEMENT, new String[]{Integer.toString(videoId)});
        HashMap<String, String> result = null;
        if(cursor.moveToNext()){
            result = new HashMap<>();
            result.put("videoId", cursor.getString(cursor.getColumnIndex("videoid")));
            result.put("tags", cursor.getString(cursor.getColumnIndex("tags")));
            result.put("key", cursor.getString(cursor.getColumnIndex("key")));
        }
        cursor.close();
        db.close();
        return result;
    }
}
