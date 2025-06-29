package com.tongxin.caihong.helper;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.tongxin.caihong.bean.AtBean;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.db.dao.RoomMemberDao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 辅助处理复制粘贴带@的群消息，
 */
public class AtCopyHelper {

    public static Map<String, Map<String, String>> sCopyAtMap = new HashMap<>();

    public static void putAtUserList(String roomId, String roomJid, String userIdListString) {
        if (TextUtils.isEmpty(roomId) || TextUtils.isEmpty(roomJid) || TextUtils.isEmpty(userIdListString)) {
            return;
        }
        Map<String, String> atMap = sCopyAtMap.get(roomId);
        if (atMap == null) {
            atMap = new HashMap<>();
            sCopyAtMap.put(roomId, atMap);
        }
        List<String> userIdList = Arrays.asList(userIdListString.split(" "));
        for (String userId : userIdList) {
            if (TextUtils.equals(userId, roomJid)) {
                // @全体成员
                atMap.put("全体成员", roomJid);
                continue;
            }
            RoomMember roomMember = RoomMemberDao.getInstance().getSingleRoomMember(roomId, userId);
            //2023.11.12 增加管理员权限
      /*      if (roomMember.getRole() == 2) {
                // @全体成员
                atMap.put("全体成员", roomJid);
                continue;
            }*/
            if (roomMember == null) {
                continue;
            }
            atMap.put(roomMember.getUserName(), userId);
        }
    }

    public static void rebuildAtUserList(String roomId, Editable editable, int start, int end, AddAtUserListener listener) {
        if (TextUtils.isEmpty(roomId) || TextUtils.isEmpty(editable)) {
            return;
        }
        if (start >= end) {
            return;
        }
        if (end > editable.length()) {
            end = editable.length();
        }
        Map<String, String> atMap = sCopyAtMap.get(roomId);
        if (atMap == null) {
            return;
        }
        CharSequence sub = editable.subSequence(start, end);
        Pattern pattern = Pattern.compile("@(\\S*)( |$)");
        Matcher matcher = pattern.matcher(sub);
        while (matcher.find()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                String nickname = matcher.group(1);
                String userId = atMap.get(nickname);
                if (TextUtils.isEmpty(userId)) {
                    continue;
                }
                ForegroundColorSpan span = new ForegroundColorSpan(Color.parseColor("#63B8FF"));
                editable.setSpan(span, matcher.start(0), matcher.end(0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                listener.addAtUser(span, new AtBean(userId));
            }
        }
    }

    public interface AddAtUserListener {
        void addAtUser(ForegroundColorSpan span, AtBean atBean);
    }
}
