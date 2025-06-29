package com.tongxin.caihong.ui.me.redpacket;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.call.JitsiInviteActivity;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.ViewHolder;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class SelectPersonActivity extends BaseActivity {

    public static void start(Context ctx, String person,String roomId) {
        Intent intent = new Intent(ctx, SelectPersonActivity .class);
        intent.putExtra("roomMember", person);
        intent.putExtra("roomId", roomId);
        ctx.startActivity(intent);
    }

    public String roomId;
    public String selectroomMember;
    private List<SelectMuMembers> mCurrentMucMembers;
    public List<RoomMember> roomMemberList;
    private ListViewAdapter mAdapter;
    private ListView inviteList;
    public EditText search_edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_person);
        roomId =  getIntent().getStringExtra("roomId");
        selectroomMember = getIntent().getStringExtra("roomMember");

         roomMemberList = RoomMemberDao.getInstance().getRoomMember(roomId);
         mCurrentMucMembers = new ArrayList<>();

        inviteList = findViewById(R.id.invitelist);
        mAdapter = new ListViewAdapter();

        inviteList.setOnItemClickListener((adapterView, view, position, l) -> {
            EventBus.getDefault().post(mCurrentMucMembers.get(position).member);
            finish();
        });

        inviteList.setAdapter(mAdapter);

        setData("");
        search_edit = findViewById(R.id.search_edit);
        search_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                setData(editable.toString());
            }
        });

    }

    public void  setData(String key){
        mCurrentMucMembers.clear();
        for (RoomMember roomMember:roomMemberList){
            if (roomMember.getUserId().equals(coreManager.getSelf().getUserId())){
                continue;
            }
            if (roomMember.getUserName().contains(key)||TextUtils.isEmpty(key)){
                SelectMuMembers selectMuMembers = new SelectMuMembers();
                selectMuMembers.setMember(roomMember);
                if (!TextUtils.isEmpty(selectroomMember)){
                    if (roomMember.getUserId().equals(selectroomMember)){
                        selectMuMembers.setChecked(true);
                    }
                }
                mCurrentMucMembers.add(selectMuMembers);
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    class SelectMuMembers {
        private RoomMember member;
        private boolean isChecked;

        public RoomMember getMember() {
            return member;
        }

        public void setMember(RoomMember member) {
            this.member = member;
        }

        public boolean isChecked() {
            return isChecked;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }
    }

    class ListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCurrentMucMembers.size();
        }

        @Override
        public Object getItem(int position) {
            return mCurrentMucMembers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_contacts_jitsi, parent, false);
            }
            CheckBox invite_ck = ViewHolder.get(convertView, R.id.invite_ck);
            ImageView invite_avatar = ViewHolder.get(convertView, R.id.invite_avatar);
            TextView invite_name = ViewHolder.get(convertView, R.id.invite_name);

            invite_ck.setChecked(mCurrentMucMembers.get(position).isChecked);
            ImageLoadHelper.showImageWithError(
                    mContext,
                    AvatarHelper.getAvatarUrl(mCurrentMucMembers.get(position).member.getUserId(), true),
                    R.drawable.avatar_normal,
                    invite_avatar
            );
            invite_name.setText(mCurrentMucMembers.get(position).getMember().getUserName());
            return convertView;
        }
    }
}