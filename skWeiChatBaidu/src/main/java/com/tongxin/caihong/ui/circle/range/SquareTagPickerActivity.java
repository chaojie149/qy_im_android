package com.tongxin.caihong.ui.circle.range;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.util.LifeCircleHelper;
import com.tongxin.caihong.util.SkinUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SquareTagPickerActivity extends BaseActivity {
    List<Item> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_square_tag_picker);

        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView title = (TextView) findViewById(R.id.tv_title_center);
        title.setText(getString(R.string.title_select_tag));
        TextView tv_title_right = (TextView) findViewById(R.id.tv_title_right);
        tv_title_right.setText(getResources().getString(R.string.finish));
        tv_title_right.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tv_title_right, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tv_title_right.setTextColor(getResources().getColor(R.color.white));
        tv_title_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                result();
            }
        });

        initView();
    }

    private void initView() {
        String selectedLabels = getIntent().getStringExtra("THIS_CIRCLE_LABLE");
        Set<String> selectedSet = LifeCircleHelper.selectedTagSet(selectedLabels);
        List<String> allTag = LifeCircleHelper.getAllTag();
        data = new ArrayList<>(allTag.size());
        int selectedIndex = -1;
        for (int i = 0; i < allTag.size(); i++) {
            String tag = allTag.get(i);
            Item item = new Item();
            item.name = tag;
            item.selected = selectedSet.contains(tag);
            if (item.selected) {
                selectedIndex = i;
            }
            data.add(item);
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
//        NoLastDividerItemDecoration itemDecoration = new NoLastDividerItemDecoration(mContext, NoLastDividerItemDecoration.VERTICAL);
//        itemDecoration.setDrawable(mContext.getDrawable(R.drawable.devider_public_message_tag));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        DividerItemDecoration divider = new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.full_divider_h_1));
        recyclerView.addItemDecoration(divider);
        Adapter adapter =new Adapter();
        adapter.setSelectIndex(selectedIndex);
        //单选多选开关
        adapter.setSupportMulti(false);
        recyclerView.setAdapter(adapter);
    }

    private void result() {
        StringBuilder sb = new StringBuilder();
        for (Item item : data) {
            if (item.selected) {
                sb.append("#").append(item.name);
            }
        }
        Intent intent = new Intent();
        intent.putExtra("THIS_CIRCLE_LABLE", sb.toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    static class Item {
        String name;
        boolean selected;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivSelected = itemView.findViewById(R.id.ivSelected);
        public TextView tvTagName = itemView.findViewById(R.id.tvTagName);

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            SkinUtils.Skin skin = SkinUtils.getSkin(itemView.getContext());
            ImageViewCompat.setImageTintList(ivSelected, ColorStateList.valueOf(skin.getAccentColor()));
        }

        public void apply(Item item) {
            tvTagName.setText(item.name);
            updateSelected(item);
        }

        public void updateSelected(Item item) {
            if (item.selected) {
                ivSelected.setVisibility(View.VISIBLE);
            } else {
                ivSelected.setVisibility(View.GONE);
            }
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private int selectIndex = -1;
        /**
         * 是否支持多选
         */
        private boolean isSupportMulti=false;

        public void setSelectIndex(int selectIndex) {
            this.selectIndex = selectIndex;
        }

        public void setSupportMulti(boolean supportMulti) {
            isSupportMulti = supportMulti;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_public_message_tag, viewGroup, false);
            itemView.setBackgroundColor(Color.WHITE);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, @SuppressLint("RecyclerView") int i) {
            Item item = data.get(i);
            viewHolder.apply(item);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (selectIndex == i || isSupportMulti) {
                        item.selected = !item.selected;
                        viewHolder.updateSelected(item);
                    }else {
                        item.selected = true;
                        viewHolder.updateSelected(item);
                        if (selectIndex!=-1){
                            Item lastSelectedItem = data.get(selectIndex);
                            lastSelectedItem.selected=false;
                            notifyItemChanged(selectIndex);
                        }
                        selectIndex = i;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
