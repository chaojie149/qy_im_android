package com.tongxin.caihong.ui.message.search;

import static com.tongxin.caihong.ui.tool.WebViewActivity.EXTRA_URL;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.mucfile.DownManager;
import com.tongxin.caihong.ui.mucfile.MucFileDetails;
import com.tongxin.caihong.ui.mucfile.XfileUtils;
import com.tongxin.caihong.ui.mucfile.bean.MucFileBean;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.filter.RegexInputFilter;
import com.tongxin.caihong.util.secure.MD5;
import com.tongxin.caihong.view.CircleImageView;
import com.tongxin.caihong.view.chatHolder.FileViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 查找指定内容
 * 文件、链接、交易
 */
public class SearchDesignationContent extends BaseActivity {

    public static int TYPE_FILE = 2;
    public static int TYPE_LINK = 3;
    public static int TYPE_PAY = 4;
    private int mSearchType;
    private String mSearchObject;
    private RecyclerView mRecyclerView;
    private DesignationContentAdapter mDesignationContentAdapter;
    private List<ChatMessage> mChatMessage = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_designation_content);
        mSearchType = getIntent().getIntExtra("search_type", TYPE_FILE);
        mSearchObject = getIntent().getStringExtra("search_objectId");

        if (mSearchType == TYPE_FILE) {
            List<ChatMessage> fileList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_FILE);
            mChatMessage.addAll(fileList);
        } else if (mSearchType == TYPE_LINK) {
            List<ChatMessage> linkList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_LINK);
            List<ChatMessage> shareLinkList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_SHARE_LINK);
            mChatMessage.addAll(linkList);
            mChatMessage.addAll(shareLinkList);
        } else if (mSearchType == TYPE_PAY) {
            List<ChatMessage> redList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_RED);
            List<ChatMessage> transferList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_TRANSFER);
            mChatMessage.addAll(redList);
            mChatMessage.addAll(transferList);
        }
        // 根据timeSend排序
        Comparator<ChatMessage> comparator = (o1, o2) -> (int) (o2.getDoubleTimeSend() - o1.getDoubleTimeSend());
        Collections.sort(mChatMessage, comparator);

        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        if (mSearchType == TYPE_FILE) {
            tvTitle.setText(getString(R.string.s_file));
        } else if (mSearchType == TYPE_LINK) {
            tvTitle.setText(getString(R.string.s_link));
        } else if (mSearchType == TYPE_PAY) {
            tvTitle.setText(getString(R.string.s_pay));
        }
    }

    private void initView() {
        mRecyclerView = findViewById(R.id.s_dest_content_rcy);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mDesignationContentAdapter = new DesignationContentAdapter(mChatMessage);
        mRecyclerView.setAdapter(mDesignationContentAdapter);
    }

    private void openFile(ChatMessage chatMessage, String type, String fileName) {
        MucFileBean mucFileBean = new MucFileBean();
        mucFileBean.setName(fileName);
        mucFileBean.setNickname(fileName);
        mucFileBean.setUrl(chatMessage.getContent());
        mucFileBean.setSize(chatMessage.getFileSize());
        mucFileBean.setState(DownManager.STATE_UNDOWNLOAD);
        mucFileBean.setType(XfileUtils.getFileType(type));
        Intent intent = new Intent(mContext, MucFileDetails.class);
        intent.putExtra("data", mucFileBean);
        startActivity(intent);
    }

    class DesignationContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<ChatMessage> mChatMessageSource;

        public DesignationContentAdapter(List<ChatMessage> chatMessages) {
            this.mChatMessageSource = chatMessages;
            if (mChatMessageSource == null) {
                mChatMessageSource = new ArrayList<>();
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new DesignationContentHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_designation, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            ChatMessage chatMessage = mChatMessageSource.get(i);
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), chatMessage.getFromUserId());
            String name = chatMessage.getFromUserName();
            if (friend != null && !TextUtils.isEmpty(friend.getRemarkName())) {
                name = friend.getRemarkName();
            }

            AvatarHelper.getInstance().displayAvatar(name, chatMessage.getFromUserId(),
                    ((DesignationContentHolder) viewHolder).mAvatarIv, true);
            if (chatMessage.getType() == XmppMessage.TYPE_TRANSFER) {
                ((DesignationContentHolder) viewHolder).mNameTv.setText(getString(R.string.start_transfer, name));
            } else {
                ((DesignationContentHolder) viewHolder).mNameTv.setText(name);
            }
            ((DesignationContentHolder) viewHolder).mDateTv.setText(TimeUtils.getFriendlyTimeDesc(mContext,
                    chatMessage.getTimeSend()));
            if (chatMessage.getType() == XmppMessage.TYPE_FILE) {
                // 文件
                fillFileData(chatMessage, ((DesignationContentHolder) viewHolder).mAbstractLl, ((DesignationContentHolder) viewHolder).mAbstractLeftIv,
                        ((DesignationContentHolder) viewHolder).mAbstractTopTv, ((DesignationContentHolder) viewHolder).mAbstractBottomTv);
            } else if (chatMessage.getType() == XmppMessage.TYPE_LINK || chatMessage.getType() == XmppMessage.TYPE_SHARE_LINK) {
                // 链接
                fillLinkData(chatMessage, ((DesignationContentHolder) viewHolder).mAbstractLl, ((DesignationContentHolder) viewHolder).mAbstractLeftIv,
                        ((DesignationContentHolder) viewHolder).mAbstractTopTv, ((DesignationContentHolder) viewHolder).mAbstractBottomTv);
            } else if (chatMessage.getType() == XmppMessage.TYPE_RED || chatMessage.getType() == XmppMessage.TYPE_TRANSFER) {
                // 红包与转账
                fillRedTransferData(chatMessage, ((DesignationContentHolder) viewHolder).mAbstractLl, ((DesignationContentHolder) viewHolder).mAbstractLeftIv,
                        ((DesignationContentHolder) viewHolder).mAbstractTopTv, ((DesignationContentHolder) viewHolder).mAbstractBottomTv);
            }
        }

        @Override
        public int getItemCount() {
            return mChatMessageSource.size();
        }

        private void fillFileData(ChatMessage chatMessage, LinearLayout ll, ImageView iv, TextView topTv, TextView bottomTv) {
            String filePath = TextUtils.isEmpty(chatMessage.getFilePath()) ? chatMessage.getContent() : chatMessage.getFilePath();
            int index = filePath.lastIndexOf(".");
            String type = filePath.substring(index + 1).toLowerCase();
            int start = filePath.lastIndexOf("/");
            String fileName = filePath.substring(start + 1).toLowerCase();

            if (type.equals("png") || type.equals("jpg")) {
                ImageLoadHelper.showImageWithError(
                        mContext,
                        FileUtil.isExist(filePath) ? filePath : chatMessage.getContent(),
                        R.drawable.image_download_fail_icon,
                        iv
                );
            } else {
                AvatarHelper.getInstance().fillFileView(type, iv);
            }
            topTv.setText(fileName);
            bottomTv.setText(XfileUtils.fromatSize(chatMessage.getFileSize()));

            ll.setOnClickListener(v -> {
                if (!FileViewHolder.unlockFileSet.contains(chatMessage.getPacketId()) && !TextUtils.isEmpty(chatMessage.getObjectId())) {
                    // 有密码，要验证，
                    DialogHelper.showLimitSingleInputDialog((Activity) mContext
                            , getString(R.string.file_password)
                            , "", getString(R.string.hint_input_file_password)
                            , new InputFilter[]{new RegexInputFilter("[a-zA-Z0-9]*")}
                            , text -> {
                                if (TextUtils.isEmpty(text)) {
                                    ToastUtil.showToast(mContext, R.string.tip_file_password_empty);
                                    return false;
                                }
                                if (!TextUtils.isEmpty(text)) {
                                    text = MD5.encryptHex(text);
                                }
                                if (!text.equals(chatMessage.getObjectId())) {
                                    ToastUtil.showToast(mContext, R.string.tip_file_password_wrong);
                                    return false;
                                }
                                FileViewHolder.unlockFileSet.add(chatMessage.getPacketId());
                                openFile(chatMessage, type, fileName);
                                return true;
                            }).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    openFile(chatMessage, type, fileName);
                }
            });
        }

        private void fillLinkData(ChatMessage chatMessage, LinearLayout ll, ImageView iv, TextView topTv, TextView bottomTv) {
            if (chatMessage.getType() == XmppMessage.TYPE_LINK) {
                // 普通链接
                try {
                    // 文件在线预览，ios分享出来的链接没有img，兼容一下，其他字段均在解析img之前解析赋值
                    JSONObject json = new JSONObject(chatMessage.getContent());
                    String linkTitle = json.getString("title");
                    topTv.setText(linkTitle);
                    String linkAddress = json.getString("url");
                    ll.setOnClickListener(v -> {
                        Intent intent = new Intent(mContext, WebViewActivity.class);
                        intent.putExtra(EXTRA_URL, linkAddress);
                        mContext.startActivity(intent);
                    });
                    String linkImage = json.getString("img");
                    ImageLoadHelper.showImageWithError(
                            mContext,
                            linkImage,
                            R.drawable.browser,
                            iv
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // SK ShareSDK 分享进来的链接
                try {
                    JSONObject json = new JSONObject(chatMessage.getObjectId());
                    String appName = json.getString("appName");
                    String appIcon = json.getString("appIcon");
                    String title = json.getString("title");
                    String subTitle = json.getString("subTitle");
                    String imageUrl = json.getString("imageUrl");
                    String linkShareAddress = json.getString("url");
                    String linkShareDownAppAddress = json.getString("downloadUrl");

                    if (TextUtils.isEmpty(appIcon) && TextUtils.isEmpty(imageUrl)) {
                        iv.setImageResource(R.drawable.browser);
                    } else if (TextUtils.isEmpty(imageUrl)) {
                        AvatarHelper.getInstance().displayUrl(appIcon, iv);
                    } else {
                        AvatarHelper.getInstance().displayUrl(imageUrl, iv);
                    }
                    topTv.setText(title);
                    bottomTv.setText(subTitle);

                    ll.setOnClickListener(v -> {
                        Intent intent = new Intent(mContext, WebViewActivity.class);
                        intent.putExtra(WebViewActivity.EXTRA_URL, linkShareAddress);
                        intent.putExtra(WebViewActivity.EXTRA_DOWNLOAD_URL, linkShareDownAppAddress);
                        mContext.startActivity(intent);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        private void fillRedTransferData(ChatMessage chatMessage, LinearLayout ll, ImageView iv, TextView topTv, TextView bottomTv) {
            if (chatMessage.getType() == XmppMessage.TYPE_RED) {
                // 红包
                iv.setImageResource(R.drawable.ic_chat_hongbao);
                topTv.setText(chatMessage.getContent());
            } else {
                // 转账
                iv.setImageResource(R.drawable.ic_tip_transfer_money);
                topTv.setText("￥ " + chatMessage.getContent());
                bottomTv.setText(chatMessage.getFilePath());
            }

            ll.setOnClickListener(v -> {
                Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), mSearchObject);
                if (friend != null) {
                    Intent intent = new Intent();
                    if (friend.getRoomFlag() == 0) { // 个人
                        intent.setClass(mContext, ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND, friend);
                    } else {
                        intent.setClass(mContext, MucChatActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                        intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
                    }

                    intent.putExtra("isserch", true);
                    intent.putExtra("jilu_id", chatMessage.getDoubleTimeSend());
                    startActivity(intent);
                }
            });
        }
    }

    class DesignationContentHolder extends RecyclerView.ViewHolder {

        private CircleImageView mAvatarIv;
        private TextView mNameTv, mDateTv;

        private LinearLayout mAbstractLl;
        private ImageView mAbstractLeftIv;
        private TextView mAbstractTopTv, mAbstractBottomTv;

        public DesignationContentHolder(@NonNull View itemView) {
            super(itemView);
            mAvatarIv = itemView.findViewById(R.id.avatar_iv);
            mNameTv = itemView.findViewById(R.id.name_tv);
            mDateTv = itemView.findViewById(R.id.date_tv);
            mAbstractLl = itemView.findViewById(R.id.abstract_ll);
            mAbstractLeftIv = itemView.findViewById(R.id.abstract_left_iv);
            mAbstractTopTv = itemView.findViewById(R.id.abstract_top_tv);
            mAbstractBottomTv = itemView.findViewById(R.id.abstract_bottom_tv);
        }
    }
}
