package com.tongxin.caihong.db.dao;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.UserAvatar;
import com.tongxin.caihong.db.SQLiteHelper;
import com.tongxin.caihong.util.TimeUtils;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import java.sql.SQLException;

/**
 * 访问用户数据的Dao，包括访问两个实体，User和UserDetail
 */
public class UserAvatarDao {
    private static UserAvatarDao instance = null;

    public static UserAvatarDao getInstance() {
        if (instance == null) {
            synchronized (UserAvatarDao.class) {
                if (instance == null) {
                    instance = new UserAvatarDao();
                }
            }
        }
        return instance;
    }

    public Dao<UserAvatar, String> userDao;

    private UserAvatarDao() {
        try {
            OrmLiteSqliteOpenHelper helper = OpenHelperManager.getHelper(MyApplication.getInstance(), SQLiteHelper.class);
            userDao = DaoManager.createDao(helper.getConnectionSource(), UserAvatar.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        OpenHelperManager.releaseHelper();
    }

    public long saveUpdateTime(String userId) {
        long time = TimeUtils.sk_time_current_time();
        QueryBuilder<UserAvatar, String> builder = userDao.queryBuilder();
        try {
            UserAvatar userAvatar = builder.where().eq("userId", userId).queryForFirst();
            if (userAvatar == null) {
                userAvatar = new UserAvatar();
                userAvatar.setUserId(userId);
                userAvatar.setTime(time);
                userDao.create(userAvatar);
            } else {
                updateUserAvatarTime(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return time;
    }

    // 更新群内昵称
    private void updateUserAvatarTime(String ownerId) {
        UpdateBuilder<UserAvatar, String> builder = userDao.updateBuilder();
        try {
            builder.where().eq("userId", ownerId);
            builder.updateColumnValue("time", TimeUtils.sk_time_current_time());
            userDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUpdateTime(String userId) {
        long time = 0l;
        QueryBuilder<UserAvatar, String> builder = userDao.queryBuilder();
        try {
            UserAvatar userAvatar = builder.where().eq("userId", userId).queryForFirst();
            if (userAvatar == null) {
                time = saveUpdateTime(userId);
            } else {
                time = userAvatar.getTime();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return String.valueOf(time);
    }
}
