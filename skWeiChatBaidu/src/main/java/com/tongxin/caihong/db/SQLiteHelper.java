package com.tongxin.caihong.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Company;
import com.tongxin.caihong.bean.Contact;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.bean.MsgRoamTask;
import com.tongxin.caihong.bean.MyPhoto;
import com.tongxin.caihong.bean.MyZan;
import com.tongxin.caihong.bean.PublicKey;
import com.tongxin.caihong.bean.UploadingFile;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.UserAvatar;
import com.tongxin.caihong.bean.VideoFile;
import com.tongxin.caihong.bean.circle.CircleMessage;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.j256.ormlite.android.AndroidDatabaseConnection;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class SQLiteHelper extends OrmLiteSqliteOpenHelper {
    public static final String DATABASE_NAME = "chat.db";
    private static final String TAG = "SQLiteHelper";
    private static final int DATABASE_VERSION = 38;

    // public static final String DATABASE_PATH = Config.SDCARD_PATH +
    // File.separator + "chat" + File.separator + "chat.db";
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * 重建数据库，
     */
    public static void rebuildDatabase(Context ctx) {
        ctx.deleteDatabase(SQLiteHelper.DATABASE_NAME);
        copyDatabaseFile(ctx);
    }

    /**
     * 如果在data目录下没有该项目数据库，则拷贝数据库
     */
    public static void copyDatabaseFile(Context context) {
        File dbFile = context.getDatabasePath(SQLiteHelper.DATABASE_NAME);
        if (dbFile.exists()) {
            return;
        }
        File parentFile = dbFile.getParentFile();
        if (!parentFile.exists()) {
            try {
                parentFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        InputStream in = null;
        FileOutputStream out = null;
        try {
            dbFile.createNewFile();
            in = context.getResources().openRawResource(R.raw.chat);
            int size = in.available();
            byte buf[] = new byte[size];
            in.read(buf);
            out = new FileOutputStream(dbFile);
            out.write(buf);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connSource) {
        createTables(connSource);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connSource, int oldVersion, int newVersion) {
        dropAllUserTables(db);
        createTables(connSource);
    }

    private void dropAllUserTables(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        //noinspection TryFinallyCanBeTryWithResources not available with API < 19
        try {
            List<String> tables = new ArrayList<>(cursor.getCount());

            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }

            for (String table : tables) {
                if (table.startsWith("sqlite_")) {
                    continue;
                }
                if (table.equals("tb_areas")) {
                    continue;
                }
                db.execSQL("DROP TABLE IF EXISTS " + table);
                Log.e(TAG, "Dropped table " + table);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * 复制的， {@link OrmLiteSqliteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)}
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ConnectionSource cs = this.getConnectionSource();
        DatabaseConnection conn = cs.getSpecialConnection();
        boolean clearSpecial = false;
        if (conn == null) {
            conn = new AndroidDatabaseConnection(db, true, this.cancelQueriesEnabled);

            try {
                cs.saveSpecialConnection((DatabaseConnection) conn);
                clearSpecial = true;
            } catch (SQLException var11) {
                throw new IllegalStateException("Could not save special connection", var11);
            }
        }

        try {
            this.onDowngrade(db, cs, oldVersion, newVersion);
        } finally {
            if (clearSpecial) {
                cs.clearSpecialConnection((DatabaseConnection) conn);
            }

        }
    }

    private void onDowngrade(SQLiteDatabase db, ConnectionSource connSource, int oldVersion, int newVersion) {
        dropAllUserTables(db);
        createTables(connSource);
    }

    private void createTables(ConnectionSource connSource) {
        try {
            TableUtils.createTableIfNotExists(connSource, Company.class);
            TableUtils.createTableIfNotExists(connSource, User.class);
            TableUtils.createTableIfNotExists(connSource, Friend.class);
            TableUtils.createTableIfNotExists(connSource, NewFriendMessage.class);
            TableUtils.createTableIfNotExists(connSource, VideoFile.class);
            TableUtils.createTableIfNotExists(connSource, MyPhoto.class);
            TableUtils.createTableIfNotExists(connSource, CircleMessage.class);
            TableUtils.createTableIfNotExists(connSource, MyZan.class);
            TableUtils.createTableIfNotExists(connSource, UserAvatar.class);
            TableUtils.createTableIfNotExists(connSource, Label.class);
            TableUtils.createTableIfNotExists(connSource, Contact.class);
            TableUtils.createTableIfNotExists(connSource, MsgRoamTask.class);
            TableUtils.createTableIfNotExists(connSource, UploadingFile.class);
            TableUtils.createTableIfNotExists(connSource, PublicKey.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void version2Drop(ConnectionSource connSource) {
        try {
            DatabaseConnection rw = connSource.getReadWriteConnection();
            rw.closeQuietly();
            TableUtils.dropTable(connSource, Company.class, false);
            TableUtils.dropTable(connSource, User.class, false);
            TableUtils.dropTable(connSource, Friend.class, false);
            TableUtils.dropTable(connSource, NewFriendMessage.class, false);
            TableUtils.dropTable(connSource, VideoFile.class, false);
            TableUtils.dropTable(connSource, MyPhoto.class, false);
            TableUtils.dropTable(connSource, CircleMessage.class, false);
            TableUtils.dropTable(connSource, MyZan.class, false);
            TableUtils.dropTable(connSource, UserAvatar.class, false);
            TableUtils.dropTable(connSource, Label.class, false);
            TableUtils.dropTable(connSource, Contact.class, false);
            TableUtils.dropTable(connSource, MsgRoamTask.class, false);
            TableUtils.dropTable(connSource, UploadingFile.class, false);
            TableUtils.dropTable(connSource, PublicKey.class, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
