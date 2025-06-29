package com.tongxin.caihong.bean.event;

/**
 * Created by Administrator on 2018/1/13 0013.
 */
public class EventNotifyByTag {
    public static final String GroupAssistant = "GroupAssistant";
    public static final String GroupAssistantKeyword = "GroupAssistantKeyword";
    public static final String Interrupt = "Interrupt_Call";
    public static final String Speak = "Speaker_Earpiece";
    public static final String Withdraw = "Withdraw_Success";
    public static final String FinishInstantRelatedActivity = "Finish_Instant_Related_Activity";
    public static final String GroupAddBlackList = "Group_Add_Black_List";

    public final String tag;

    public EventNotifyByTag(String tag) {
        this.tag = tag;
    }
}
