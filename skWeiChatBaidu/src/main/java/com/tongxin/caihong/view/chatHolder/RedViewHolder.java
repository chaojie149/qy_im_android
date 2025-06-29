package com.tongxin.caihong.view.chatHolder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.redpacket.EventRedReceived;
import com.tongxin.caihong.bean.redpacket.OpenRedpacket;
import com.tongxin.caihong.bean.redpacket.RedDialogBean;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.redpacket.RedDetailsActivity;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.NoDoubleClickListener;
import com.tongxin.caihong.R;
import com.tongxin.caihong.view.redDialog.RedDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

class RedViewHolder extends AChatHolderInterface {

    TextView mTvContent;
    TextView mTvType;

    private RedDialog mRedDialog;
    private ImageView iv_image;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_redpacket : R.layout.chat_to_item_redpacket;
    }

    @Override
    public void initView(View view) {
        mTvContent = view.findViewById(R.id.chat_text);
        mTvType = view.findViewById(R.id.tv_type);
        mRootView = view.findViewById(R.id.chat_warp_view);
        iv_image = view.findViewById(R.id.iv_image);
    }

    @Override
    public void fillData(ChatMessage message) {
        String s = StringUtils.replaceSpecialChar(message.getContent());
        CharSequence charSequence = HtmlUtils.transform200SpanString(s, true);
        String typeRed;
        if (TextUtils.equals(message.getFilePath(), String.valueOf(1))) {
            typeRed = getString(R.string.usual_gift);
        } else if (TextUtils.equals(message.getFilePath(), String.valueOf(2))) {
            typeRed = getString(R.string.red_random);
        } else if (TextUtils.equals(message.getFilePath(), String.valueOf(3))) {
            typeRed = getString(R.string.mes_gift);
        }else if (TextUtils.equals(message.getFilePath(), String.valueOf(5))) {
           typeRed=message.getToUserName().split("_")[0] +"专属红包";
        }
        else {
            typeRed = getString(R.string.red_envelope);
        }
        if (mdata.getFileSize() == 2) {// 已领取
            mRootView.setAlpha(0.6f);
        } else {
            mRootView.setAlpha(1f);
        }
        mTvContent.setText(charSequence);
        mTvType.setText(typeRed);

        mRootView.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                RedViewHolder.super.onClick(view);
            }
        });
    }

    @Override
    public boolean isOnClick() {
        return false; // 红包消息点击后回去请求接口，所以要做一个多重点击替换
    }

    @Override
    protected void onRootClick(View v) {
        if (TextUtils.equals(mdata.getFilePath(), String.valueOf(3))) {
            if (mdata.isDownload()) {
                // 当前正在群组信息-未领完红包界面，该界面暂不支持领取口令红包
                ToastUtil.showToast(mContext, getString(R.string.tip_not_support_received_kl_red));
                return;
            }
        }
        clickRedpacket();
    }

    // 点击红包
    public void clickRedpacket() {
        if (selfGroupRole != null && MucRoomMember.disallowPublicAction(selfGroupRole)) {
            ToastUtil.showToast(mContext, getString(R.string.tip_action_disallow_place_holder, getString(MucRoomMember.getRoleName(selfGroupRole))));
            return;
        }
        final String redId = mdata.getObjectId();

        HashMap<String, String> params = new HashMap<>();
        params.put("id", redId);

        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HttpUtils.get().url(CoreManager.requireConfig(mContext).RENDPACKET_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getData() != null) {
                            // 当resultCode==1时，表示可领取
                            // 当resultCode==0时，表示红包已过期、红包已退回、红包已领完
                            int resultCode = result.getResultCode();
                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 0);
                            if (!TextUtils.isEmpty(result.getResultMsg())) //resultMsg不为空表示红包已过期
                            {
                                bundle.putInt("timeOut", 1);
                            } else {
                                bundle.putInt("timeOut", 0);
                            }

                            bundle.putBoolean("isGroup", isGounp);
                            bundle.putString("mToUserId", mToUserId);
                            intent.putExtras(bundle);

                            // 红包不可领取, 或者我发的单聊红包直接跳转
                            if (resultCode != 1 || (!isGounp && isMysend)) {
                                if (resultCode != 1) {
                                    // 处理漫游下来的红包消息，如已经领取再次点击变为已领取状态
                                    if (openRedpacket.getPacket() != null
                                            && openRedpacket.getPacket().getUserIds().contains(mLoginUserId)) {
                                        mdata.setFileSize(2);
                                        ChatMessageDao.getInstance().updateChatMessageReceiptStatus(mLoginUserId, mToUserId, mdata.getPacketId());
                                        fillData(mdata);
                                    }
                                }
                                mContext.startActivity(intent);
                            } else {
                                // 在群里面我领取过的红包直接跳转
                                if (isGounp && mdata.getFileSize() != 1) {
                                    mContext.startActivity(intent);
                                } else {
                                    if (mdata.getFilePath().equals("3")) {
                                        // 口令红包编辑输入框
                                        changeBottomViewInputText(mdata.getContent());
                                    } else {
                                        RedDialogBean redDialogBean = new RedDialogBean(openRedpacket.getPacket().getUserId(), openRedpacket.getPacket().getUserName(),
                                                openRedpacket.getPacket().getGreetings(), openRedpacket.getPacket().getId());
                                        mRedDialog = new RedDialog(mContext, redDialogBean, new RedDialog.OnClickRedListener() {
                                            @Override
                                            public void clickRed() {
                                                openRedPacket(openRedpacket.getPacket());
                                            }

                                            @Override
                                            public void clickTail() {
                                                showRedReceivedDetail(redId);
                                            }
                                        }, isMysend);
                                        mRedDialog.chatMessage = mdata;
                                        mRedDialog.show();
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    // 打开红包
    private void openRedPacket(OpenRedpacket.PacketEntity packetEntity) {
        String redId = mdata.getObjectId();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", redId);
        String url;
        if (!TextUtils.isEmpty(packetEntity.getuPayRedPacketId())) {
            if (!WeboxHelper.checkOpenedOrAsk(mContext)) {
                if (mRedDialog != null) {
                    mRedDialog.dismiss();
                }
                return;
            }
            url = CoreManager.requireConfig(mContext).WEBOX_ACCEPT_RED;
        } else {
            url = CoreManager.requireConfig(mContext).REDPACKET_OPEN;
        }

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (mRedDialog != null) {
                            mRedDialog.dismiss();
                        }
                        if (result.getData() != null) {
                            mdata.setFileSize(2);
                            ChatMessageDao.getInstance().updateChatMessageReceiptStatus(mLoginUserId, mToUserId, mdata.getPacketId());
                            fillData(mdata);
                            // 新增获取群组十分钟后未领完的红包功能，
                            // 为一个单独的页面，如果在那个页面领取了红包，群聊页面也要及时更新领取状态，发个广播去更新吧
                            Intent receivedIntent = new Intent(OtherBroadcast.ACTION_GROUP_RECEIVED_RED_PACKET);
                            receivedIntent.putExtra(AppConstant.EXTRA_MSG_ID, mdata.getPacketId());
                            mContext.sendBroadcast(receivedIntent);

                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 1);
                            bundle.putInt("timeOut", 0);

                            bundle.putBoolean("isGroup", isGounp);
                            bundle.putString("mToUserId", mToUserId);
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                            // 更新余额
                            CoreManager.updateMyBalance();

                            if (!TextUtils.equals(mLoginUserId, openRedpacket.getPacket().getUserId())
                                    && !isGounp) {
                                EventBus.getDefault().post(new EventRedReceived(openRedpacket));
                            }
                        } else {
                            Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (mRedDialog != null) {
                            mRedDialog.dismiss();
                        }
                    }
                });
    }

    // 查看红包领取详情
    private void showRedReceivedDetail(String redId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("id", redId);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).RENDPACKET_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (result.getData() != null) {
                            // 当resultCode==1时，表示可领取
                            // 当resultCode==0时，表示红包已过期、红包已退回、红包已领完
                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 0);
                            if (!TextUtils.isEmpty(result.getResultMsg())) //resultMsg不为空表示红包已过期
                            {
                                bundle.putInt("timeOut", 1);
                            } else {
                                bundle.putInt("timeOut", 0);
                            }

                            bundle.putBoolean("isGroup", true);
                            bundle.putString("mToUserId", mToUserId);
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                        } else {
                            Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    // 通知更新输入框
    private void changeBottomViewInputText(String text) {
        mHolderListener.onChangeInputText(text);
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }
}
