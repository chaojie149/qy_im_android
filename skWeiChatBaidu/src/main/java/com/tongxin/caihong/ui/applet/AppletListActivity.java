package com.tongxin.caihong.ui.applet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

public class AppletListActivity extends BaseActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, AppletListActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applet_list);
        initView();
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> {
            finish();
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.applet));

        View search = findViewById(R.id.search_edit);
        search.setOnClickListener(v -> {
            AppletSearchActivity.start(mContext);
        });

        Fragment fragment = new AppletListFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.flFragment, fragment)
                .commit();
    }

}
