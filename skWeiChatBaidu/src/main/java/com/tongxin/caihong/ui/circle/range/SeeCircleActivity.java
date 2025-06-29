package com.tongxin.caihong.ui.circle.range;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.LabelDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.util.LifeCircleHelper;
import com.tongxin.caihong.ui.contacts.label.CreateLabelActivity;
import com.tongxin.caihong.ui.contacts.label.SelectLabelFriendActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.CommonAdapter;
import com.tongxin.caihong.util.CommonViewHolder;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 谁可以看
 */
public class SeeCircleActivity extends BaseActivity implements View.OnClickListener {
    public static final int SELECT_LABEL_FRIEND_REQUEST_CODE = 10000;
    public static final int CREATE_LABEL_REQUEST_CODE = 10001;
    private static final List<Integer> typeOrder = Arrays.asList(5, 1, 2, 3, 4);
    private ImageView iv_sel1, iv_sel2, iv_sel3, iv_sel4, iv_sel6;
    private List<ImageView> imageViews;
    private RecyclerView rvTag;
    private ListView lv1, lv2;
    private SeeCircleAdapter mSeeCircleAdapter;
    private List<Label> mLabelList;
    // 记录被选中的标签id
    private List<String> mSelectPositions;
    // 从‘通讯录选择’ 选中的userIdList与userNameList
    private List<String> mUserIdList;
    private List<String> mUserNameList;
    private String mLoginUserId;

    // 记录当前选中的发布说说方式，默认为公开
    private int currentSelected;
    // 用于展开/收起 lv1,lv2列表
    private boolean flag1, flag2;
    private String str1;
    private String str2;
    private String str3;
    private int mCurrentLabelPosition;
    private String selectedLabels;
    private List<TagItem> tagData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.see_circle_activity);

        int selectedType = getIntent().getIntExtra("THIS_CIRCLE_TYPE", 5);
        currentSelected = typeOrder.indexOf(selectedType);
        str1 = getIntent().getStringExtra("THIS_CIRCLE_PERSON_RECOVER1");
        str2 = getIntent().getStringExtra("THIS_CIRCLE_PERSON_RECOVER2");
        str3 = getIntent().getStringExtra("THIS_CIRCLE_PERSON_RECOVER3");
        selectedLabels = getIntent().getStringExtra("THIS_CIRCLE_LABLE");

        initAction();
        loadData();
        initView();
        initEvent();
    }

    private void initAction() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView title = (TextView) findViewById(R.id.tv_title_center);
        title.setText(getString(R.string.who_can_see));
        TextView title_right = (TextView) findViewById(R.id.tv_title_right);
        title_right.setText(getString(R.string.finish));
        findViewById(R.id.tv_title_right).setOnClickListener(this);
    }

    private void loadData() {
        imageViews = new ArrayList<>();
        mLabelList = new ArrayList<>();
        mSelectPositions = new ArrayList<>();
        mUserIdList = new ArrayList<>();
        mUserNameList = new ArrayList<>();

        mLoginUserId = coreManager.getSelf().getUserId();
        mLabelList = LabelDao.getInstance().getAllLabels(mLoginUserId);
        Label label = new Label();
        label.setGroupId("0x01");
        mLabelList.add(label);
    }

    private void initView() {
        iv_sel1 = (ImageView) findViewById(R.id.iv_sel1);
        iv_sel2 = (ImageView) findViewById(R.id.iv_sel2);
        iv_sel3 = (ImageView) findViewById(R.id.iv_sel3);
        iv_sel4 = (ImageView) findViewById(R.id.iv_sel4);
        iv_sel6 = (ImageView) findViewById(R.id.iv_sel6);
        imageViews.add(iv_sel1);
        imageViews.add(iv_sel6);
        imageViews.add(iv_sel2);
        imageViews.add(iv_sel3);
        imageViews.add(iv_sel4);
        for (ImageView view : imageViews) {
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(SkinUtils.getSkin(mContext).getAccentColor()));
        }

        lv1 = (ListView) findViewById(R.id.lv1);
        lv2 = (ListView) findViewById(R.id.lv2);
        mSeeCircleAdapter = new SeeCircleAdapter(this, mLabelList);
        lv1.setAdapter(mSeeCircleAdapter);
        lv2.setAdapter(mSeeCircleAdapter);

        rvTag = findViewById(R.id.rvTag);
        Set<String> selectedSet = LifeCircleHelper.selectedTagSet(selectedLabels);
        List<String> allTag = LifeCircleHelper.getAllTag();
        tagData = new ArrayList<>(allTag.size());
        for (String tag : allTag) {
            TagItem item = new TagItem();
            item.name = tag;
            item.selected = selectedSet.contains(tag);
            tagData.add(item);
        }
        TagAdapter tagAdapter = new TagAdapter(tagData);
        rvTag.setAdapter(tagAdapter);

        setSelected(currentSelected);
        if (currentSelected == 3 || currentSelected == 4) {
            List<String> string1 = JSON.parseArray(str1, String.class);
            List<String> string2 = JSON.parseArray(str2, String.class);
            List<String> string3 = JSON.parseArray(str3, String.class);
            mSelectPositions.addAll(string1);
            mUserIdList.addAll(string2);
            mUserNameList.addAll(string3);
            mSeeCircleAdapter.notifyDataSetChanged();
        }
    }

    private void initEvent() {
        findViewById(R.id.rl_public).setOnClickListener(this);
        findViewById(R.id.rl_friend).setOnClickListener(this);
        findViewById(R.id.rl_private).setOnClickListener(this);
        findViewById(R.id.rl_not_all).setOnClickListener(this);
        findViewById(R.id.rl_not_see).setOnClickListener(this);
        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                String groupId = mLabelList.get(position).getGroupId();
                if (groupId.equals("0x01")) {
                    Intent intent1 = new Intent(SeeCircleActivity.this, SelectLabelFriendActivity.class);
                    intent1.putExtra("IS_FROM_SEE_CIRCLE_ACTIVITY", true);
                    intent1.putExtra("exist_ids", JSON.toJSONString(mUserIdList));
                    startActivityForResult(intent1, SELECT_LABEL_FRIEND_REQUEST_CODE);
                } else {
                    if (hasSelected(groupId)) {
                        removeSelect(groupId);
                    } else {
                        addSelect(groupId);
                    }
                }
            }
        });

        lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                String groupId = mLabelList.get(position).getGroupId();
                if (groupId.equals("0x01")) {
                    Intent intent1 = new Intent(SeeCircleActivity.this, SelectLabelFriendActivity.class);
                    intent1.putExtra("IS_FROM_SEE_CIRCLE_ACTIVITY", true);
                    intent1.putExtra("exist_ids", JSON.toJSONString(mUserIdList));
                    startActivityForResult(intent1, SELECT_LABEL_FRIEND_REQUEST_CODE);
                } else {
                    if (hasSelected(groupId)) {
                        removeSelect(groupId);
                    } else {
                        addSelect(groupId);
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.tv_title_right:
                // 完成
                Intent intent = new Intent();
                StringBuilder sb = new StringBuilder();
                for (TagItem item : tagData) {
                    if (item.selected) {
                        sb.append("#").append(item.name);
                    }
                }
                intent.putExtra("THIS_CIRCLE_LABLE", sb.toString());
                if (currentSelected == 3 || currentSelected == 4) {
                    // 部分可见 || 不给谁看
                    if (mSelectPositions.size() > 0 || mUserIdList.size() > 0) {
                        String ids = getSelected();
                        if (TextUtils.isEmpty(ids)) {
                            // 可能选中的是空标签，也不能让他发送
                            ToastUtil.showToast(mContext, getString(R.string.tip_select_at_lease_one_contacts));
                            return;
                        }
                        intent.putExtra("THIS_CIRCLE_TYPE", typeOrder.get(currentSelected));
                        intent.putExtra("THIS_CIRCLE_PERSON", getSelected());
                        intent.putExtra("THIS_CIRCLE_PERSON_NAME", getSelectedName());
                        // Todo 部分可见 ||  不给谁看,需要将以下参数也返回，下次进入恢复选中状态
                        intent.putExtra("THIS_CIRCLE_PERSON_RECOVER1", JSON.toJSONString(mSelectPositions));
                        intent.putExtra("THIS_CIRCLE_PERSON_RECOVER2", JSON.toJSONString(mUserIdList));
                        intent.putExtra("THIS_CIRCLE_PERSON_RECOVER3", JSON.toJSONString(mUserNameList));

                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                        ToastUtil.showToast(SeeCircleActivity.this, R.string.tip_select_at_least_one);
                    }
                } else {
                    // 公开 || 私密
                    intent.putExtra("THIS_CIRCLE_TYPE", typeOrder.get(currentSelected));
                    Log.e("zq", "currentSelected:" + currentSelected);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                break;
            case R.id.rl_public:
                setSelected(0);
                break;
            case R.id.rl_friend:
                setSelected(1);
                break;
            case R.id.rl_private:
                setSelected(2);
                break;
            case R.id.rl_not_all:
                setSelected(3);
                break;
            case R.id.rl_not_see:
                setSelected(4);
                break;
        }
    }

    // 得到'部分可见' || '不给谁看'的人的id,','号拼接
    private String getSelected() {
        String userIdList = "";
        List<String> list = new ArrayList<>();
        // 1.先添加选中的label内的userId
        for (int i = 0; i < mSelectPositions.size(); i++) {
            for (int j = 0; j < mLabelList.size(); j++) {
                if (mLabelList.get(j).getGroupId().equals(mSelectPositions.get(i))) {
                    List<String> strings = JSON.parseArray(mLabelList.get(j).getUserIdList(), String.class);
                    if (strings != null && !strings.isEmpty()) {
                        list.addAll(strings);
                    }
                }
            }
        }
        // 2.在添加未建立标签的选中的userId
        list.addAll(mUserIdList);
        // 3.去重
        List<String> newList = new ArrayList<>(new HashSet<>(list));
        // List<String> newList = new ArrayList<>(new TreeSet<>(list));// 可去重并且排序
        // 4.在拼接为服务器需要的格式
        for (int i = 0; i < newList.size(); i++) {
            if (i == newList.size() - 1) {
                userIdList += newList.get(i);
            } else {
                userIdList += newList.get(i) + ",";
            }
        }
        return userIdList;
    }

    // 记录(标记)当前选中的发布说说方式
    private void setSelected(int position) {
        mSelectPositions.clear();
        mUserIdList.clear();
        mUserNameList.clear();

        for (int i = 0; i < imageViews.size(); i++) {
            if (i == position) {
                currentSelected = position;
                imageViews.get(i).setVisibility(View.VISIBLE);
            } else {
                imageViews.get(i).setVisibility(View.INVISIBLE);
            }
        }
        if (position == 0) {
            rvTag.setVisibility(View.VISIBLE);
        } else {
            rvTag.setVisibility(View.GONE);
        }
        if (position == 0 || position == 1 || position == 2) {
            lv1.setVisibility(View.GONE);
            lv2.setVisibility(View.GONE);
            flag1 = false;
            flag2 = false;
        } else if (position == 3) {
            flag1 = !flag1;
            if (flag1) {
                lv1.setVisibility(View.VISIBLE);
                lv2.setVisibility(View.GONE);
                flag2 = false;
            } else {
                lv1.setVisibility(View.GONE);
                lv2.setVisibility(View.GONE);
            }
            mSeeCircleAdapter.notifyDataSetChanged();
        } else if (position == 4) {
            flag2 = !flag2;
            if (flag2) {
                lv1.setVisibility(View.GONE);
                lv2.setVisibility(View.VISIBLE);
                flag1 = false;
            } else {
                lv1.setVisibility(View.GONE);
                lv2.setVisibility(View.GONE);
            }
            mSeeCircleAdapter.notifyDataSetChanged();
        }
    }

    // 得到'部分可见' || '不给谁看'的人的名字,'、'号拼接
    private String getSelectedName() {
        String userNameList = "";
        List<String> list = new ArrayList<>();
        // 1.先添加选中的labelName
        for (int i = 0; i < mSelectPositions.size(); i++) {
            for (int j = 0; j < mLabelList.size(); j++) {
                if (mLabelList.get(j).getGroupId().equals(mSelectPositions.get(i))) {
                    list.add(mLabelList.get(j).getGroupName());
                }
            }
        }
        // 2.在添加未建立标签的选中的userName
        list.addAll(mUserNameList);
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                userNameList += list.get(i);
            } else {
                userNameList += list.get(i) + ",";
            }
        }
        return userNameList;
    }

    // ITEM OPERATING
    private boolean hasSelected(String groupId) {
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i).equals(groupId)) {
                return true;
            }
        }
        return false;
    }

    private void addSelect(String groupId) {
        mSelectPositions.add(groupId);
        mSeeCircleAdapter.notifyDataSetChanged();
    }

    private void removeSelect(String groupId) {
        mSelectPositions.remove(groupId);
        mSeeCircleAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_LABEL_FRIEND_REQUEST_CODE && resultCode == RESULT_OK) {
            String labelId = data.getStringExtra("NEW_LABEL_ID");
            if (!TextUtils.isEmpty(labelId)) {
                Label label = LabelDao.getInstance().getLabel(mLoginUserId, labelId);
                mLabelList.add(0, label);
                mSelectPositions.add(labelId);// 选中
                mSeeCircleAdapter.notifyDataSetChanged();
            } else {// 未存标签
                String userIds = data.getStringExtra("inviteId");
                String userNames = data.getStringExtra("inviteName");
                mUserIdList = JSON.parseArray(userIds, String.class);
                mUserNameList = JSON.parseArray(userNames, String.class);
                mSeeCircleAdapter.notifyDataSetChanged();
            }
        } else if (requestCode == CREATE_LABEL_REQUEST_CODE && resultCode == RESULT_OK) {
            Label label = LabelDao.getInstance().getLabel(mLoginUserId, mLabelList.get(mCurrentLabelPosition).getGroupId());
            mLabelList.remove(mCurrentLabelPosition);
            mLabelList.add(mCurrentLabelPosition, label);
            mSeeCircleAdapter.notifyDataSetChanged();
        }
    }

    static class TagItem {
        String name;
        boolean selected;
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect = itemView.findViewById(R.id.cbSelect);
        TextView tvTagName = itemView.findViewById(R.id.tvTagName);

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            ButtonColorChange.tintCheckBox(cbSelect);
        }

        public void apply(TagItem item) {
            itemView.setOnClickListener(v -> {
                item.selected = !item.selected;
                cbSelect.setChecked(item.selected);
            });
            tvTagName.setText(item.name);
            cbSelect.setChecked(item.selected);
        }
    }

    static class TagAdapter extends RecyclerView.Adapter<TagViewHolder> {
        private List<TagItem> data;

        TagAdapter(List<TagItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_select_see_circle_tag, viewGroup, false);
            return new TagViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull TagViewHolder viewHolder, int i) {
            TagItem item = data.get(i);
            viewHolder.apply(item);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    class SeeCircleAdapter extends CommonAdapter<Label> {

        SeeCircleAdapter(Context context, List<Label> data) {
            super(context, data);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.row_select_see_circle, position);
            CheckBox mGroupSelectCb = viewHolder.getView(R.id.see_check_box);
            ButtonColorChange.tintCheckBox(mGroupSelectCb);
            mGroupSelectCb.setChecked(false);
            TextView mGroupNameTv = viewHolder.getView(R.id.label_name);
            TextView mGroupUserNameTv = viewHolder.getView(R.id.label_user_name);
            ImageView mEditLabelIv = viewHolder.getView(R.id.edit_label_iv);
            final Label label = data.get(position);
            if (label != null) {
                if (label.getGroupId().equals("0x01")) {
                    mGroupSelectCb.setVisibility(View.INVISIBLE);
                    mGroupNameTv.setTextColor(getResources().getColor(R.color.link_nick_name_color));
                    ButtonColorChange.textChange(mContext, mGroupUserNameTv);
                    mEditLabelIv.setVisibility(View.GONE);
                    mGroupNameTv.setText(R.string.select_from_contacts);
                    String userNames = "";
                    if (mUserNameList.size() > 0) {
                        for (int i = 0; i < mUserNameList.size(); i++) {
                            if (i == mUserNameList.size() - 1) {
                                userNames += mUserNameList.get(i);
                            } else {
                                userNames += mUserNameList.get(i) + "，";
                            }
                        }
                        mGroupUserNameTv.setVisibility(View.VISIBLE);
                        mGroupUserNameTv.setText(userNames);
                    } else {
                        mGroupUserNameTv.setVisibility(View.GONE);
                    }
                } else {
                    mGroupSelectCb.setVisibility(View.VISIBLE);
                    mGroupNameTv.setTextColor(getResources().getColor(R.color.app_black));
                    mGroupUserNameTv.setTextColor(getResources().getColor(R.color.Grey_400));
                    mGroupUserNameTv.setVisibility(View.VISIBLE);
                    mEditLabelIv.setVisibility(View.VISIBLE);
                    for (int i = mSelectPositions.size() - 1; i >= 0; i--) {
                        if (mSelectPositions.get(i).equals(label.getGroupId())) {
                            mGroupSelectCb.setChecked(true);
                        }
                    }
                    mGroupNameTv.setText(label.getGroupName());
                    List<String> userIds = JSON.parseArray(label.getUserIdList(), String.class);
                    String userNames = "";
                    if (userIds != null && userIds.size() > 0) {
                        for (int i = 0; i < userIds.size(); i++) {
                            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userIds.get(i));
                            if (friend != null) {
                                if (i == userIds.size() - 1) {
                                    userNames += TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
                                } else {
                                    userNames += TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() + "，" : friend.getRemarkName() + "，";
                                }
                            }
                        }
                        mGroupUserNameTv.setText(userNames);
                    } else {
                        mGroupUserNameTv.setVisibility(View.GONE);
                    }
                }
            }

            mEditLabelIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCurrentLabelPosition = position;

                    Intent intent = new Intent(SeeCircleActivity.this, CreateLabelActivity.class);
                    intent.putExtra("isEditLabel", true);
                    intent.putExtra("labelId", label.getGroupId());
                    intent.putExtra("IS_FROM_SEE_CIRCLE_ACTIVITY", true);
                    startActivityForResult(intent, CREATE_LABEL_REQUEST_CODE);
                }
            });
            return viewHolder.getConvertView();
        }
    }

}
