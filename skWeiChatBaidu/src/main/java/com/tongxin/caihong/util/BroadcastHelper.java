package com.tongxin.caihong.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class BroadcastHelper {
    private BroadcastHelper() {
    }

    public static void register(LifecycleOwner owner, BroadcastReceiver receiver, String... actions) {
        Context ctx;
        if (owner instanceof Activity) {
            ctx = (Context) owner;
        } else if (owner instanceof Fragment) {
            ctx = ((Fragment) owner).requireContext();
        } else {
            throw new IllegalArgumentException("require Context");
        }
        owner.getLifecycle().addObserver(new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            void create() {
                IntentFilter filter = new IntentFilter();
                for (String action : actions) {
                    filter.addAction(action);
                }
                ctx.registerReceiver(receiver, filter);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void destroy() {
                ctx.unregisterReceiver(receiver);
            }
        });
    }
}
