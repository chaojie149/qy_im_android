package com.tongxin.caihong.ui.me;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.BaseRecAdapter;
import com.tongxin.caihong.ui.base.BaseRecViewHolder;
import com.tongxin.caihong.util.RecyclerSpace;
import com.tongxin.caihong.util.SkinUtils;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.List;

/**
 * Created by zq on 2017/8/26 0026.
 * <p>
 * 更换皮肤
 */
public class SkinStore extends BaseActivity {
    private final List<SkinUtils.Skin> skins = SkinUtils.defaultSkins;
    private SkinAdapter skinAdapter;
    private SkinUtils.Skin currentSkin;
    private int currentSkinIndex = -1;
    private int cSelectSkinIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_switch_skin);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.change_skin));
        TextView tvTitleRight = findViewById(R.id.tv_title_right);
        tvTitleRight.setText(getString(R.string.save));
        tvTitleRight.setOnClickListener(view -> {
            if (currentSkinIndex == cSelectSkinIndex) {
                finish();
                return;
            }
            SkinUtils.setSkin(SkinStore.this, currentSkin);
            MainActivity.isInitView = true;
            Intent intent = new Intent(SkinStore.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    protected void initView() {
        // 当前皮肤
        currentSkin = SkinUtils.getSkin(this);
        SwipeRecyclerView swipeRecyclerView = findViewById(R.id.rv_pager);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        swipeRecyclerView.setLayoutManager(layoutManager);
        skinAdapter = new SkinAdapter(skins);
        swipeRecyclerView.setAdapter(skinAdapter);
        swipeRecyclerView.addItemDecoration(new RecyclerSpace(8, Color.TRANSPARENT, 1));
    }

    class SkinAdapter extends BaseRecAdapter<SkinUtils.Skin, SkiinViewHolder> {
        SkinAdapter(List<SkinUtils.Skin> data) {
            super(data);
        }

        @Override
        public void onHolder(SkiinViewHolder holder, SkinUtils.Skin bean, int position) {
            holder.skinName.setText(bean.getColorName());
            ViewCompat.setBackgroundTintList(holder.skinName, ColorStateList.valueOf(bean.getPrimaryColor()));
            if (bean.isLight()) {
                holder.skinName.setTextColor(holder.skinName.getContext().getResources().getColor(R.color.black));
            }
            ImageViewCompat.setImageTintList(holder.skinIv, ColorStateList.valueOf(bean.getAccentColor()));
            if (currentSkin == bean) {
                if (currentSkinIndex == -1) {
                    currentSkinIndex = position;
                }
                cSelectSkinIndex = position;
                holder.skinCheck.setVisibility(View.VISIBLE);
            } else {
                holder.skinCheck.setVisibility(View.GONE);
            }
        }

        @Override
        public SkiinViewHolder onCreateHolder() {
            return new SkiinViewHolder(getViewByRes(R.layout.item_switch_skin_new));
        }
    }

    public class SkiinViewHolder extends BaseRecViewHolder implements View.OnClickListener {
        public TextView skinName;
        public ImageView skinIv;
        public ImageView skinCheck;

        public SkiinViewHolder(View rootView) {
            super(rootView);
            skinName = rootView.findViewById(R.id.tv_color_name);
            skinIv = rootView.findViewById(R.id.iv_color);
            skinCheck = rootView.findViewById(R.id.check);
            rootView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // 减1一个头布局
            int position = getAdapterPosition();
            currentSkin = skins.get(position);
            skinAdapter.notifyDataSetChanged();
        }
    }
}
