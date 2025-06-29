package com.tongxin.caihong.ui.contacts.label;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.LabelDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.PullToRefreshSlideListView;
import com.tongxin.caihong.view.SelectionFrame;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * A simple {@link Fragment} subclass.
 */
public class LabelFragment extends EasyFragment {
    private PullToRefreshSlideListView prsLabel;
    private LabelAdapter labelAdapter;
    private List<Label> data = new ArrayList<>();

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_label;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initView();
            refreshLabelListFromService();
        }
    }

    private void initView() {
        prsLabel = findViewById(R.id.prsLabel);
        labelAdapter = new LabelAdapter(mContext);
        prsLabel.getRefreshableView().setAdapter(labelAdapter);

        prsLabel.setOnRefreshListener(refreshView -> refreshLabelListFromService());
        prsLabel.getRefreshableView().setOnItemClickListener((parent, view, position, id) -> {
            if (UiUtils.isNormalClick(view)) {
                position = (int) id;
                Label label = data.get(position);
                if (label != null) {
                    Intent intent = new Intent(requireActivity(), CreateLabelActivity.class);
                    intent.putExtra("isEditLabel", true);
                    intent.putExtra("labelId", label.getGroupId());
                    startActivityForResult(intent, 0x01);
                }
            }
        });

        ButtonColorChange.colorChange(mContext, findViewById(R.id.btnCreateLabel));
        findViewById(R.id.btnCreateLabel).setOnClickListener(view -> {
            Intent intent = new Intent(mContext, CreateLabelActivity.class);
            intent.putExtra("isEditLabel", false);
            startActivityForResult(intent, 0x01);
        });
    }

    private void refreshLabelListFromService() {
        Map<String, String> params = new HashMap<>();

        HttpUtils.get().url(coreManager.getConfig().FRIENDGROUP_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Label>(Label.class) {
                    @Override
                    public void onResponse(ArrayResult<Label> result) {
                        prsLabel.onRefreshComplete();
                        if (Result.checkSuccess(mContext, result)) {
                            List<Label> labelList = result.getData();
                            if (labelList == null) {
                                labelList = new ArrayList<>();
                            }
                            LabelDao.getInstance().refreshLabel(coreManager.getSelf().getUserId(), labelList);
                            data = labelList;
                            labelAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        loadData();
                    }
                });
    }

    private void loadData() {
        data.clear();
        data = LabelDao.getInstance().getAllLabels(coreManager.getSelf().getUserId());
        labelAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x01) {
            loadData();
        }
    }

    class LabelAdapter extends SlideBaseAdapter {

        public LabelAdapter(Context context) {
            super(context);
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.row_label;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.row_item_lable_delete;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            TextView tvName = ViewHolder.get(convertView, R.id.tvName);
            TextView tvNum = ViewHolder.get(convertView, R.id.tvNum);
            TextView tvUsersName = ViewHolder.get(convertView, R.id.tvUsersName);
            ImageView ivDelete = ViewHolder.get(convertView, R.id.ivDelete);
            final Label label = data.get(position);
            if (label != null) {
                tvName.setText(label.getGroupName());
                List<String> userIds = JSON.parseArray(label.getUserIdList(), String.class);
                if (userIds != null) {
                    tvNum.setText("(" + userIds.size() + ")");
                } else {
                    tvNum.setText("(0)");
                }
                tvUsersName.setText("");
                StringBuilder usersName = new StringBuilder();
                if (userIds != null && userIds.size() > 0) {
                    for (int i = 0; i < userIds.size(); i++) {
                        Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), userIds.get(i));
                        if (friend != null) {
                            if (i == userIds.size() - 1) {
                                usersName.append(friend.getShowName());
                            } else {
                                usersName.append(friend.getShowName()).append("、");
                            }
                        }
                    }
                    tvUsersName.setText(usersName.toString());
                }
            }
            ivDelete.setOnClickListener(v -> showDeleteLabelDialog(label));
            return convertView;
        }

        private void showDeleteLabelDialog(final Label label) {
            SelectionFrame selectionFrame = new SelectionFrame(requireContext());
            selectionFrame.setSomething(getString(R.string.delete_label)
                    , getString(R.string.ask_delete_label)
                    , new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {

                        }

                        @Override
                        public void confirmClick() {
                            deleteLabel(label);
                        }
                    });
            selectionFrame.show();
        }

        private void deleteLabel(final Label label) {
            DialogHelper.showDefaulteMessageProgressDialog(requireActivity());
            Map<String, String> params = new HashMap<>();
            params.put("groupId", label.getGroupId());
            HttpUtils.get().url(coreManager.getConfig().FRIENDGROUP_DELETE)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {
                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (Result.checkSuccess(mContext, result)) {
                                LabelDao.getInstance().deleteLabel(coreManager.getSelf().getUserId(), label.getGroupId());
                                data.remove(label);
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        }
    }
}
