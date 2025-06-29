package com.tongxin.caihong.helper;

import android.content.Context;

import com.tongxin.caihong.R;
import com.tongxin.caihong.view.BottomListDialog;

import java.util.ArrayList;
import java.util.List;

public class PayTypeHelper {

    public static void selectPayType(Context ctx, SelectPayTypeCallback callback) {
        List<String> data = new ArrayList<>();
        data.add(ctx.getString(R.string.my_purse));
        if (WeboxHelper.ENABLE) {
            data.add(ctx.getString(R.string.my_webox));
        }
        if (data.size() == 1) {
            callback.payType(PayType.values()[0]);
            return;
        }
        BottomListDialog.show(ctx, data, (item, position) -> {
            callback.payType(PayType.values()[position]);
        });
    }

    public enum PayType {
        DEFAULT, WEBOX
    }

    public interface SelectPayTypeCallback {
        void payType(PayType type);
    }
}
