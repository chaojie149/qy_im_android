package com.tongxin.caihong.ui.message.assistant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.assistant.GroupAssistant;
import com.tongxin.caihong.bean.assistant.GroupAssistantDetail;
import com.tongxin.caihong.bean.assistant.KeyWord;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

public class GroupAssistantDetailActivity extends BaseActivity implements View.OnClickListener {

    private String roomId, roomJid, helperId;
    private GroupAssistant groupAssistant;
    private GroupAssistantDetail groupAssistantDetail;

    private ListView mKeyWordLv;
    private KeyWordAdapter mKeyWordAdapter;
    private List<KeyWord> mKeyWordData = new ArrayList<>();

    public static void start(Context context, String roomId, String roomJid, String groupAssistant) {
        Intent intent = new Intent(context, GroupAssistantDetailActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomJid", roomJid);
        intent.putExtra("groupAssistant", groupAssistant);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_assistant_detail);

        roomId = getIntent().getStringExtra("roomId");
        roomJid = getIntent().getStringExtra("roomJid");
        String str = getIntent().getStringExtra("groupAssistant");
        groupAssistant = JSON.parseObject(str, GroupAssistant.class);
        if (groupAssistant == null) {
            Toast.makeText(mContext, getString(R.string.tip_get_detail_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        helperId = groupAssistant.getId();

        initActionBar();
        initView();
        initData();
        initEvent();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(groupAssistant.getName());
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        ImageView avatar = findViewById(R.id.iv1);
        TextView name = findViewById(R.id.tv1);
        TextView desc = findViewById(R.id.tv2);
        TextView developer = findViewById(R.id.tv3);
        TextView disclaimer = findViewById(R.id.tv4);
        AvatarHelper.getInstance().displayUrl(groupAssistant.getIconUrl(), avatar);
        name.setText(groupAssistant.getName());
        desc.setText(groupAssistant.getDesc());
        developer.setText(getString(R.string.developer, groupAssistant.getDeveloper()));
        disclaimer.setText(getString(R.string.disclaimer, groupAssistant.getDeveloper()));

        ButtonColorChange.colorChange(mContext, findViewById(R.id.btn1));
        ButtonColorChange.colorChange(mContext, findViewById(R.id.btn2));

        mKeyWordLv = findViewById(R.id.lv);
        mKeyWordAdapter = new KeyWordAdapter();
        mKeyWordLv.setAdapter(mKeyWordAdapter);
    }

    /**
     * 获取当前群组已经添加的群助手
     * params-helperId(获取已添加群助手内指定的某一个群助手详情)
     */
    private void initData() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("roomId", roomId);
        params.put("helperId", helperId);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).ROOM_QUERY_GROUP_HELPER)
                .params(params)
                .build()
                .execute(new ListCallback<GroupAssistantDetail>(GroupAssistantDetail.class) {
                    @Override
                    public void onResponse(ArrayResult<GroupAssistantDetail> result) {
                        if (result != null && result.getResultCode() == 1) {
                            if (result.getData().size() > 0) {
                                groupAssistantDetail = result.getData().get(0);
                                mKeyWordData = groupAssistantDetail.getKeyWords();
                                if (mKeyWordAdapter != null) {
                                    mKeyWordAdapter.notifyDataSetChanged();
                                }
                                changeVisible(true);
                            } else {
                                changeVisible(false);
                            }
                        } else {
                            changeVisible(false);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void initEvent() {
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.iv2).setOnClickListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, EventNotifyByTag.GroupAssistantKeyword)) {
            initData();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                addGroupAssistant();
                break;
            case R.id.btn2:
                deleteGroupAssistant();
                break;
            case R.id.btn3:
                GroupAssistantAddKeywordActivity.start(mContext, roomId, JSON.toJSONString(groupAssistantDetail));
                break;
            case R.id.iv2:
                GroupAssistantAddKeywordActivity.start(mContext, roomId, JSON.toJSONString(groupAssistantDetail));
                break;
        }
    }

    private void changeVisible(boolean visible) {
        if (visible) {
            findViewById(R.id.btn1).setVisibility(View.GONE);
            findViewById(R.id.btn2).setVisibility(View.VISIBLE);
            findViewById(R.id.ll1).setVisibility(View.VISIBLE);
            if (mKeyWordData.size() > 0) {
                findViewById(R.id.ll2).setVisibility(View.GONE);
                mKeyWordLv.setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.ll2).setVisibility(View.VISIBLE);
                mKeyWordLv.setVisibility(View.GONE);
            }
        } else {
            findViewById(R.id.btn1).setVisibility(View.VISIBLE);
            findViewById(R.id.btn2).setVisibility(View.GONE);
            findViewById(R.id.ll1).setVisibility(View.GONE);
            findViewById(R.id.ll2).setVisibility(View.GONE);
            mKeyWordLv.setVisibility(View.GONE);
            mKeyWordData.clear();
            mKeyWordAdapter.notifyDataSetChanged();
        }
    }

    private void addGroupAssistant() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("helperId", groupAssistant.getId());
        params.put("roomId", roomId);
        params.put("roomJid", roomJid);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).ROOM_ADD_GROUP_HELPER)
                .params(params)
                .build()
                .execute(new BaseCallback<GroupAssistantDetail>(GroupAssistantDetail.class) {

                    @Override
                    public void onResponse(ObjectResult<GroupAssistantDetail> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result != null && result.getResultCode() == 1) {
                            groupAssistantDetail = result.getData();
                            Toast.makeText(mContext, getString(R.string.add_to) + getString(R.string.success), Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.GroupAssistant));
                            changeVisible(true);
                        } else {
                            if (result != null && !TextUtils.isEmpty(result.getResultMsg())) {
                                ToastUtil.showToast(mContext, result.getResultMsg());
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void deleteGroupAssistant() {
        if (groupAssistantDetail == null) {
            ToastUtil.showToast(mContext, getString(R.string.tip_server_error));
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("groupHelperId", groupAssistantDetail.getId());

        HttpUtils.get().url(CoreManager.requireConfig(mContext).ROOM_DELETE_GROUP_HELPER)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result != null && result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.delete) + getString(R.string.success), Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.GroupAssistant));
                            changeVisible(false);
                        } else {
                            if (result != null && !TextUtils.isEmpty(result.getResultMsg())) {
                                ToastUtil.showToast(mContext, result.getResultMsg());
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    class KeyWordAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mKeyWordData.size();
        }

        @Override
        public Object getItem(int position) {
            return mKeyWordData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_group_assistant_keyword, parent, false);
            }
            TextView keywordTv = ViewHolder.get(convertView, R.id.keyword_tv);

            KeyWord keyWord = mKeyWordData.get(position);
            if (keyWord != null) {
                keywordTv.setText(keyWord.getKeyword());
            }
            return convertView;
        }
    }
}
