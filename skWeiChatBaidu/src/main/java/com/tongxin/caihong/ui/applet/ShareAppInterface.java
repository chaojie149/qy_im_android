package com.tongxin.caihong.ui.applet;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * js 通信接口
 */
public class ShareAppInterface {
    private static final String TAG = "ShareAppInterface";

    private AppletActivity context;
    private WebView app;

    public ShareAppInterface(AppletActivity context, WebView app) {
        this.context = context;
        this.app = app;
    }

    /**
     * js-调用分享  object.gameId,object.title,object.imgUrl,object.des,object.appName,object.appInco
     *
     * @param gameId  小游戏id
     * @param title   小游戏标题
     * @param imgUrl  小游戏分享图片地址
     * @param appName 小游戏名称
     * @param appInco 小游戏图标地址
     */
    @JavascriptInterface
    public void shareMessage(String gameId, String title, String imgUrl, String appName, String appInco) {
        Log.d(TAG, "shareMessage() called with: gameId = [" + gameId + "], title = [" + title + "], imgUrl = [" + imgUrl + "], appName = [" + appName + "], appInco = [" + appInco + "]");
        // 这里写拉起分享的代码

        context.runOnUiThread(() -> {
            context.shareMessage(gameId, title, imgUrl, appName, appInco);
        });
    }


    /**
     * js-调用支付  object.gameId, object.mode, object.currencyType, object.title, object.money
     *
     * @param gameId       游戏id
     * @param mode         支付类型
     * @param currencyType 币种
     * @param title        支付标题
     * @param money        金额(单价)
     * @param buyQuantity  数量
     */

    @JavascriptInterface
    public void requestPayment(String gameId, String mode, String currencyType, String title, float money, int buyQuantity) {
        Log.d(TAG, "requestPayment() called with: gameId = [" + gameId + "], mode = [" + mode + "], currencyType = [" + currencyType + "], title = [" + title + "], money = [" + money + "], buyQuantity = [" + buyQuantity + "]");

        context.runOnUiThread(() -> {
            context.requestPayment(gameId, mode, currencyType, title, money, buyQuantity);
        });
    }


    public void onShareSuccess(String msg) {
        if (app != null) {
            String str = String.format("javascript:onShareSuccess('%s')", msg);


            app.loadUrl(str);
        }
    }


    public void onShareError(String msg) {

        if (app != null) {
            String str = String.format("javascript:onShareError('%s')", msg);
            app.loadUrl(str);
        }
    }

    public void onPaySuccess(String msg) {

        if (app != null) {
            String str = String.format("javascript:onPaySuccess('%s')", msg);
            app.loadUrl(str);
        }
    }

    public void onPayError(String msg) {
        if (app != null) {
            String str = String.format("javascript:onPayError('%s')", msg);
            app.loadUrl(str);
        }
    }
}
