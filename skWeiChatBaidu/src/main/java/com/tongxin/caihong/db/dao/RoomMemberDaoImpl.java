package com.tongxin.caihong.db.dao;

import com.tongxin.caihong.bean.RoomMember;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * Create by zq
 */
// OrmLite动态创建表，一个实体类创建多张表的的偏招
public class RoomMemberDaoImpl extends BaseDaoImpl<RoomMember, Integer> {
    public RoomMemberDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<RoomMember> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
