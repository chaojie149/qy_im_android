package com.tongxin.caihong.bean.event;

import com.tongxin.caihong.bean.MsgRoamTask;

/**
 * 离线消息过多创建漫游任务时通知群聊页面准备拉漫游，
 */
public class EventCreateMucRoomTask {
    public MsgRoamTask msgRoamTask;

    public EventCreateMucRoomTask(MsgRoamTask msgRoamTask) {

        this.msgRoamTask = msgRoamTask;
    }
}
