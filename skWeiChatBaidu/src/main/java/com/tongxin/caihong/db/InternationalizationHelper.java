package com.tongxin.caihong.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.Emoji;
import com.tongxin.caihong.bean.EmojiComp;
import com.tongxin.caihong.bean.Prefix;
import com.tongxin.caihong.util.LocaleHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Administrator on 2017/4/12 0012.
 * 与ios统一，使用数据库进行国际化
 */

public class InternationalizationHelper {

    // 改这个数据库内容同时修改数据库文件名，以便覆盖安装时更新数据库文件，
    public static final String DB_NAME = "constant_2.db"; //保存的数据库文件名
    private static InternationalizationHelper helper = new InternationalizationHelper();
    private final int BUFFER_SIZE = 400000;
    /**
     * 缓存打开的数据库，避免每次都打开，因此不能用完就close关闭，
     * 打开大约5到20毫秒，
     */
    private SQLiteDatabase db;

    private InternationalizationHelper() {
    }

    public static InternationalizationHelper getInternationalizationHelper() {
        return helper;
    }

    /**
     * 国际化
     *
     * @param ios
     * @return
     */
    public static String getString(String ios) {
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "lang";
            String[] columns = new String[]{"zh", "en", "big5"};
            String selection = "ios=?";
            String[] selectionArgs = new String[]{ios};
            // 只查一条数据时limit 1更快一点，
            Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, null, "1");

            String ms = LocaleHelper.getPersistedData(MyApplication.getContext(), Locale.getDefault().getLanguage());
            String language = " ";

            // 仅有一条记录时使用moveToFirst比moveToNext快，
            if (cursor.moveToFirst()) {
                if (ms.equals("zh")) {
                    language = cursor.getString(cursor.getColumnIndex("zh"));
                } else if (ms.equals("HK") || ms.equals("TW")) {
                    language = cursor.getString(cursor.getColumnIndex("big5"));
                } else {
                    language = cursor.getString(cursor.getColumnIndex("en"));
                }
            }
            cursor.close();
            return language;
        }
        return null;
    }

    /*
    查询SMS_country,返回所有数据
     */
    public static List<Prefix> getPrefixList() {
        List<Prefix> prefixList = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "SMS_country";
            Cursor cursor = db.query(table, null, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                Prefix preFix = new Prefix();
                String country = cursor.getString(cursor.getColumnIndex("country"));
                String enName = cursor.getString(cursor.getColumnIndex("enName"));
                int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                preFix.setCountry(country);
                preFix.setEnName(enName);
                preFix.setPrefix(prefix);
                prefixList.add(preFix);
            }
            cursor.close();
        }
        return prefixList;
    }

    /*
   查询emoji,返回所有数据
    */
    public static List<Emoji> getEmojiList() {
        List<Emoji> emojis = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "emoji";
            Cursor cursor = db.query(table, null, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                Emoji emoji = new Emoji();
                String filename = cursor.getString(cursor.getColumnIndex("filename"));
                String english = cursor.getString(cursor.getColumnIndex("english"));
                int count = cursor.getInt(cursor.getColumnIndex("count"));
                emoji.setFilename(filename);
                emoji.setEnglish(english);
                emoji.setCount(count);
                emojis.add(emoji);
            }

            Collections.sort(emojis, new EmojiComp());
            cursor.close();
        }
        return emojis;
    }

    /*
   查询单个emoji,返回count数据
    */
    public static void getEmojiString(String emoji) {
        SQLiteDatabase db = helper.openDatabase();
        int i = 0;
        if (db != null) {
            String table = "emoji";
            String selection = "english=?";
            String[] selectionArgs = new String[]{emoji};
            Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);
            while (cursor.moveToNext()) {
                i = cursor.getInt(cursor.getColumnIndex("count"));
            }
            getEmoji(i, emoji);
            cursor.close();
        }
    }

    private static void getEmoji(int i, String emoji) {
        SQLiteDatabase db = helper.openDatabase();
        String table = "emoji";
        ContentValues values = new ContentValues();
        values.put("count", i + 1);
        Log.e("zx", "getEmoji: " + i + "  " + emoji);
        db.update(table, values, "english=?", new String[]{emoji});
    }

    /*
    查询SMS_country,返回所有查询数据
     */
    public static List<Prefix> getSearchPrefix(String Selection) {
        List<Prefix> prefixList = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "SMS_country";
            String selection = "country like ? or enName like ?";
            Selection = "%" + Selection + "%";
            String[] selectionArgs = new String[]{Selection, Selection};
            Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

            while (cursor.moveToNext()) {
                Prefix preFix = new Prefix();
                String country = cursor.getString(cursor.getColumnIndex("country"));
                String enName = cursor.getString(cursor.getColumnIndex("enName"));
                int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                preFix.setCountry(country);
                preFix.setEnName(enName);
                preFix.setPrefix(prefix);
                prefixList.add(preFix);
            }
            cursor.close();
        }
        return prefixList;
    }

    public static Dao<Area, Integer> getAreaDao() throws SQLException {
        return DaoManager.createDao(new AndroidConnectionSource(helper.openDatabase()), Area.class);
    }

    private SQLiteDatabase openDatabase() {
        if (db != null) {
            return db;
        }
        synchronized (this) {
            if (db != null) {
                return db;
            }
            try {
                File dbfile = MyApplication.getContext().getDatabasePath(DB_NAME);
                if (!(dbfile.exists())) {
                    //判断数据库文件是否存在，若不存在则执行导入，否则直接打开数据库
                    InputStream is = MyApplication.getContext().getResources().openRawResource(
                            R.raw.constant); //欲导入的数据库

                    FileOutputStream fos = new FileOutputStream(dbfile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count = 0;
                    while ((count = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, count);
                    }
                    fos.close();
                    is.close();
                }

                db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);
                return db;

            } catch (FileNotFoundException e) {
                Log.e("Database", "File not found");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("Database", "IO exception");
                e.printStackTrace();
            }
        }
        return null;
    }
}
