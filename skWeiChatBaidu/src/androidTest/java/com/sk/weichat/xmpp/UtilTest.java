package com.tongxin.caihong.xmpp;

import android.util.LruCache;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UtilTest {
    final LruCache<String, String> mMsgIDMap = new LruCache<>(200);
    List<String> idList = new ArrayList<>();

    @Before
    public void init() {
        for (int i = 0; i < 5000; i++) {
            idList.add(UUID.randomUUID().toString());
        }
    }

    @Test
    public void idMapTest() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            String id = idList.get(i % idList.size());
            save(id);
        }
        long end = System.currentTimeMillis();
        System.out.println("LruCache sync: " + (end - start)); // 117
    }

    private void save(String id) {
        synchronized (mMsgIDMap) {
            if (mMsgIDMap.get(id) != null) {
                return;
            }
            mMsgIDMap.put(id, id);
        }
    }
}
