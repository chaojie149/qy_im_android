package com.tongxin.caihong.view.chatHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.bean.AppletShare;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.applet.AppletActivity;
import com.tongxin.caihong.R;

class AppletViewHolder extends AChatHolderInterface {

    TextView tvLinkTitle;
    ImageView ivLinkImage;
    ImageView ivLinkInco;
    TextView tvLinkAppName;

    String appUrl;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_applet : R.layout.chat_to_item_applet;
    }

    @Override
    public void initView(View view) {
        tvLinkTitle = view.findViewById(R.id.link_title_tv);
        ivLinkImage = view.findViewById(R.id.link_iv);
        ivLinkInco = view.findViewById(R.id.link_app_icon_iv);
        tvLinkAppName = view.findViewById(R.id.link_app_name_tv);

        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        AppletShare applet = JSONObject.parseObject(message.getContent(), AppletShare.class);
        String appName = applet.getAppName();
        String appIcon = applet.getAppIcon();
        String imageUrl = applet.getImageUrl();
        String title = applet.getTitle();
        appUrl = applet.getAppUrl();

        tvLinkAppName.setText(appName);
        AvatarHelper.getInstance().displayUrl(appIcon, ivLinkInco);
        tvLinkTitle.setText(title);

        if (TextUtils.isEmpty(appIcon) && TextUtils.isEmpty(imageUrl)) {
            ivLinkImage.setImageResource(R.drawable.browser);
        } else if (TextUtils.isEmpty(imageUrl)) {
            AvatarHelper.getInstance().displayUrl(appIcon, ivLinkImage);
        } else {
            AvatarHelper.getInstance().displayUrl(imageUrl, ivLinkImage);
        }
    }

    @Override
    public void showTime(String time) {

    }

    @Override
    protected void onRootClick(View v) {
        AppletActivity.start(mContext, appUrl);
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }
}
