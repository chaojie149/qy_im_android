package com.tongxin.caihong.ui.applet;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.bean.Applet;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.R;

public class AppletViewHolder extends RecyclerView.ViewHolder {
    private View vDivider = itemView.findViewById(R.id.vDivider);
    private ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
    private TextView tvName = itemView.findViewById(R.id.tvName);

    public AppletViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void apply(Applet item, int position) {
        if (position == 0) {
            vDivider.setVisibility(View.GONE);
        } else {
            vDivider.setVisibility(View.VISIBLE);
        }
        AvatarHelper.getInstance().displayUrl(item.getAppIcon(), ivIcon);
        tvName.setText(item.getAppName());
    }
}
