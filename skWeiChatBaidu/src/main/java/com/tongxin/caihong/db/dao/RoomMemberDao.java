package com.tongxin.caihong.db.dao;

import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.db.SQLiteHelper;
import com.tongxin.caihong.db.SQLiteRawUtil;
import com.tongxin.caihong.db.SQLiteRawUtil2;
import com.tongxin.caihong.ui.base.CoreManager;
import com.j256.ormlite.android.DatabaseTableConfigUtil;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Create by zq
 */
//访问用户群组的Dao
public class RoomMemberDao {
    private static RoomMemberDao instance = null;
    private SQLiteHelper mHelper;
    private Map<String, Dao<RoomMember, Integer>> mDaoMap;
    private DatabaseTableConfig<RoomMember> mConfig;

    private RoomMemberDao() {
        mHelper = OpenHelperManager.getHelper(MyApplication.getInstance(), SQLiteHelper.class);
        mDaoMap = new HashMap<String, Dao<RoomMember, Integer>>();
    }

    public static final RoomMemberDao getInstance() {
        if (instance == null) {
            synchronized (RoomMemberDao.class) {
                if (instance == null) {
                    instance = new RoomMemberDao();
                }
            }
        }
        return instance;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        OpenHelperManager.releaseHelper();
    }

    private Dao<RoomMember, Integer> getDao(String roomId) {
        return getDao(roomId, true);
    }

    private Dao<RoomMember, Integer> getDao(String roomId, boolean createIfNotExists) {
        if (TextUtils.isEmpty(roomId)) {
            return null;
        }
        String tableName = SQLiteRawUtil2.MEMBER_TABLE_PREFIX + roomId;
        if (mDaoMap.containsKey(tableName)) {
            return mDaoMap.get(tableName);
        }
        Dao<RoomMember, Integer> dao = null;
        try {
            DatabaseTableConfig<RoomMember> config;
            if (mConfig == null) {
                config = DatabaseTableConfigUtil.fromClass(mHelper.getConnectionSource(), RoomMember.class);
                config.setTableName(tableName);
                mConfig = config;
            } else {
                config = new DatabaseTableConfig<>(mConfig.getDataClass(), tableName, mConfig.getFieldConfigs());
            }
            if (createIfNotExists) {
                mHelper.getWritableDatabase().execSQL(SQLiteRawUtil2.getCreateRoomMemberTableSql(tableName));
            }
            dao = new RoomMemberDaoImpl(mHelper.getConnectionSource(), config);
        } catch (SQLException e) {
            if (createIfNotExists) {
                // 不创建表导致的异常无视，
                e.printStackTrace();
            }
        }
        if (dao != null)
            mDaoMap.put(tableName, dao);
        return dao;
    }
    /***************************
     * 增
     * *************************
     */
    /**
     * 添加 || 更新 一个群成员
     */
    public void saveSingleRoomMember(String roomId, RoomMember roomMember) {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            // 暂无该表
            return;
        }
        try {
            new TransactionManager(dao.getConnectionSource()).callInTransaction(() -> {
                RoomMember member = getSingleRoomMember(roomId, roomMember.getUserId());
                if (member != null) {
                    roomMember.set_id(member.get_id());
                    dao.update(roomMember);
                } else {
                    // 保存该成员信息
                    dao.create(roomMember);
                }
                // 这里没用到返回值，但是这个方法必须返回，
                return Void.class;
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /***************************
     * 删
     * *************************
     */

    /**
     * 删除某个成员
     */
    public boolean deleteRoomMember(String roomId, String userId) {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            return false;
        }
        try {
            List<RoomMember> roomMembers = dao.queryForEq("userId", userId);
            if (roomMembers != null && roomMembers.size() > 0) {
                dao.delete(roomMembers);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * 删除表
     */
    public void deleteRoomMemberTable(String roomId) {
        String tableName = SQLiteRawUtil2.MEMBER_TABLE_PREFIX + roomId;
        if (mDaoMap.containsKey(tableName)) {
            mDaoMap.remove(tableName);
        }
        if (SQLiteRawUtil.isTableExist(mHelper.getWritableDatabase(), tableName)) {
            SQLiteRawUtil.dropTable(mHelper.getWritableDatabase(), tableName);
        }
        // 删除表只在刷新群成员列表时调用，所以直接在这里标记群头像过期，
        UserAvatarDao.getInstance().saveUpdateTime(roomId);
    }

    /***************************
     * 改
     * *************************
     */
    /**
     * 更新职位
     */
    public void updateRoomMemberRole(String roomId, String userId, int role) {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            Log.e("zq", "更新失败");
            return;
        }
        UpdateBuilder<RoomMember, Integer> builder = dao.updateBuilder();
        try {
            Log.e("zq", "更改表：member_" + roomId);
            builder.updateColumnValue("role", role);
            builder.where().eq("userId", userId);
            dao.update(builder.prepare());
            Log.e("zq", "更新完成");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (TextUtils.equals(userId, CoreManager.requireSelf(MyApplication.getContext()).getUserId())) {
            // 更新朋友表，以便群组页查询，
            FriendDao.getInstance().updateRoomRole(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), roomId, role);
        }
    }

    /**
     * 更新备注名
     */
    public void updateRoomMemberCardName(String roomId, String userId, String cardName) {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            Log.e("zq", "更新失败");
            return;
        }
        UpdateBuilder<RoomMember, Integer> builder = dao.updateBuilder();
        try {
            Log.e("zq", "更改表：member_" + roomId);
            builder.updateColumnValue("cardName", cardName);
            builder.where().eq("userId", userId);
            dao.update(builder.prepare());
            Log.e("zq", "更新完成");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新群昵称，
     */
    public void updateRoomMemberUserName(String roomId, String userId, String userName) {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            Log.e("zq", "更新失败");
            return;
        }
        RoomMember member = getSingleRoomMember(roomId, userId);
        if (member == null) {
            return;
        }
        UpdateBuilder<RoomMember, Integer> builder = dao.updateBuilder();
        try {
            Log.e("zq", "更改表：member_" + roomId);
            builder.updateColumnValue("userName", userName);
            if (TextUtils.equals(member.getCardName(), member.getUserName())) {
                // 两个相等认为是没有群主备注，这时候cardName要跟着改，
                builder.updateColumnValue("cardName", userName);
            }
            builder.where().eq("userId", userId);
            dao.update(builder.prepare());
            Log.e("zq", "更新完成");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /***************************
     * 查
     * *************************
     */
    /**
     * 获取表内某条数据
     */
    public RoomMember getSingleRoomMember(String roomId, String userId) {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            return null;
        }
        QueryBuilder<RoomMember, Integer> builder = dao.queryBuilder();
        RoomMember mRoomMember = null;
        try {
            if (!TextUtils.isEmpty(userId)) {
                builder.where().eq("userId", userId);
            }
            mRoomMember = dao.queryForFirst(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mRoomMember;
    }

    /**
     * 获取表内所有数据
     */
    public List<RoomMember> getRoomMember(String roomId) {
        List<RoomMember> roomMembers = new ArrayList<>();
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            return roomMembers;
        }
        try {
            roomMembers = dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (roomMembers == null) {
            roomMembers = new ArrayList<>();
        }
        return roomMembers;
    }

    /**
     * 获取表内所有数据
     */
    public int getRoomMemberCount(String roomId) {
        try {
            Dao<RoomMember, Integer> dao = getDao(roomId);
            return Integer.parseInt(dao.queryBuilder()
                    .selectRaw("ifnull(count(*), 0)").queryRawFirst()[0]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取成员用于群转让群主，
     * 所以要过滤掉群主，
     */
    public List<RoomMember> getRoomMemberForTransfer(String roomId, String selfId) throws SQLException {
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            return new ArrayList<>();
        }
        QueryBuilder<RoomMember, Integer> builder = dao.queryBuilder();
        builder.where().ne("userId", selfId)
                .eq("role", 2)
                .or().eq("role", 3)
                .and(2);
        return builder.query();
    }
    /**
     * 获取群主和群管理成员用于推送邀请申请，
     */
    public List<RoomMember> getRoomMemberAndAdminForList(String roomId)  {
        List<RoomMember> roomMembers = new ArrayList<>();
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            return new ArrayList<>();
        }
        QueryBuilder<RoomMember, Integer> builder = dao.queryBuilder();
        try {
            builder.where().eq("role", 1)
                    .or().eq("role", 2);
            roomMembers= builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return roomMembers;
    }
    public List<RoomMember> getRoomMemberForAvatar(String roomId, String selfId) throws SQLException {
        Objects.requireNonNull(roomId);
        Objects.requireNonNull(selfId);
        Dao<RoomMember, Integer> dao = getDao(roomId);
        if (dao == null) {
            throw new IllegalStateException("获取DAO失败<" + roomId + ", " + selfId + ">");
        }
        // select * from table where userId != :selfId limit 5;
        QueryBuilder<RoomMember, Integer> builder = dao.queryBuilder()
                .orderBy("createTime", true);
        // 群组头像最多为5人组合
        builder.limit(5L);
        return builder.query();
    }

    /**
     * 群主优先显示群主备注
     */
    public String getRoomRemarkName(String roomJid, String userId) {
        Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), roomJid);
        if (friend != null) {
            RoomMember mMember = getSingleRoomMember(friend.getRoomId(), CoreManager.requireSelf(MyApplication.getContext()).getUserId());
            if (mMember != null && mMember.getRole() == 1) {
                RoomMember member = getSingleRoomMember(friend.getRoomId(), userId);
                if (member != null && !TextUtils.equals(member.getUserName(), member.getCardName())) {
                    // 当userName与cardName不一致时，我们认为群主有设置群内备注(也只有群主的才会不一样)
                    return member.getCardName();
                }
            }
        }
        return null;
    }

    public RoomMember searchMemberContains(Friend friend, String str) {
        String fuzzySearchKey = "%" + str + "%";
        Dao<RoomMember, Integer> dao = getDao(friend.getRoomId(), false);
        if (dao == null) {
            throw new IllegalStateException("获取DAO失败<" + friend.getRoomId() + ">");
        }
        try {
            return dao.queryBuilder()
                    .limit(1L)
                    .where().like("userName", fuzzySearchKey)
                    .or().like("cardName", fuzzySearchKey)
                    .queryForFirst();
        } catch (SQLException e) {
            // 表不存在情况抛异常无视，
            return null;
        }
    }

    /**
     * 获取群成员昵称
     */
    public String getRoomMemberName(String roomJid, String userId) {
        Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), roomJid);
        if (friend != null) {
            RoomMember member = getSingleRoomMember(friend.getRoomId(), userId);
            if (member != null) {
                return member.getUserName();
            }
        }
        return "";
    }
}
