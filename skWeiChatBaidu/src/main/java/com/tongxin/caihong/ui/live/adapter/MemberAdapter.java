package com.tongxin.caihong.ui.live.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.adapter.BaseListAdapter;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.live.bean.Member;

public class MemberAdapter extends BaseListAdapter<Member> {
    public MemberAdapter(Context content) {
        super(content);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_menber, parent, false);
            viewHolder.avatar = (ImageView) convertView.findViewById(R.id.ivAvatar);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Member member = datas.get(position);
        AvatarHelper.getInstance().displayAvatar(String.valueOf(member.getUserId()), viewHolder.avatar, true);
        return convertView;
    }

     class ViewHolder {
        ImageView avatar;
    }
}
