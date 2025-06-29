package com.tongxin.caihong.view.chatHolder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.R;

import org.json.JSONException;
import org.json.JSONObject;

class TextImgViewHolder extends AChatHolderInterface {

    TextView tvTitle;  // 主标题
    TextView tvText;   // 副标题
    ImageView ivImage; // 图像
    String mLinkUrl;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_text_img : R.layout.chat_to_item_text_img;
    }

    @Override
    public void initView(View view) {
        tvTitle = view.findViewById(R.id.chat_title);
        tvText = view.findViewById(R.id.chat_text);
        ivImage = view.findViewById(R.id.chat_img);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        try {
            JSONObject json = new JSONObject(message.getContent());
            String tile = json.getString("title");
            String sub = json.getString("sub");
            String img = json.getString("img");

            if (message.getType() == XmppMessage.TYPE_IMAGE_TEXT_HTML) {
                String contentId = json.getString("contentId");
                mLinkUrl = CoreManager.requireConfig(mContext).apiUrl + "mp/showContent.html?access_token="
                        + CoreManager.requireSelfStatus(mContext).accessToken
                        + "&contentId=" + contentId + "&userId=" + mLoginUserId;
            } else {
                mLinkUrl = json.getString("url");
            }

            tvTitle.setText(tile);
            tvText.setText(sub);
            AvatarHelper.getInstance().displayUrl(img, ivImage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRootClick(View v) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, mLinkUrl);
        mContext.startActivity(intent);
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }
}
