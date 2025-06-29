package com.tongxin.caihong.ui.account;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.db.dao.UserDao;
import com.tongxin.caihong.helper.AccountSwitchHelper;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.window.WindowShowService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import okhttp3.Call;

public class AccountSwitchFragment extends EasyFragment {

    private TextView tvTitle1;
    private TextView tvTitle2;
    private SwipeRecyclerView recyclerView;
    private boolean editMode;
    private Adapter adapter;

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_account_switch;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        initView();
        loadData();
    }

    @Override
    public void onDestroy() {
        AccountSwitchHelper.setInSwitching(null);
        super.onDestroy();
    }

    private void initView() {
        recyclerView = findViewById(R.id.recyclerView);
        View header = LayoutInflater.from(requireContext()).inflate(R.layout.header_account_switch, recyclerView, false);
        tvTitle1 = header.findViewById(R.id.tvTitle1);
        tvTitle2 = header.findViewById(R.id.tvTitle2);
        View footer = LayoutInflater.from(requireContext()).inflate(R.layout.footer_account_switch, recyclerView, false);
        recyclerView.addHeaderView(header);
        recyclerView.addFooterView(footer);
    }

    // 退出当前账号
    private void showExitDialog() {
        SelectionFrame mSF = new SelectionFrame(requireContext());
        mSF.setSomething(null, getString(R.string.sure_exit_account), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                AccountSwitchHelper.setInSwitching(null);
                quit();
                AsyncUtils.postDelayed(requireActivity(), r -> {
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    r.finish();
                }, 200);
            }
        });
        mSF.show();
    }

    private void loadData() {
        AsyncUtils.doAsync(this, c -> {
            Set<String> exists = AccountSwitchHelper.load(requireContext());
            String current = UserSp.getInstance(requireContext()).getUserId("");
            List<Item> data = new ArrayList<>(exists.size());
            for (String userId : exists) {
                User user = UserDao.getInstance().getUserByUserId(userId);
                if (user == null) {
                    continue;
                }
                Item item = new Item();
                item.userId = userId;
                item.nickname = user.getNickName();
                item.account = user.getTelephoneNoAreaCode();
                if (TextUtils.equals(current, userId)) {
                    item.checked = true;
                }
                data.add(item);
            }
            adapter = new Adapter(data, editMode, new OnItemClickListener() {
                @Override
                public void onAccountClick(Item item) {
                    if (item.checked) {
                        // 当前账号点击不处理，
                        return;
                    }
                    c.uiThread(r -> {
                        DialogHelper.showDefaulteMessageProgressDialog(requireContext());
                    });
                    AccountSwitchFragment.this.quit();
                    AccountSwitchHelper.loadOldUser(requireContext(), item.userId);
                    coreManager.setSelf(null);
                    AsyncUtils.postDelayed(AccountSwitchFragment.this, r -> {
                        AccountSwitchFragment.this.jump();
                    }, 200);
                }

                @Override
                public void onAddClick() {
                    AccountSwitchHelper.setInSwitching(AccountSwitchFragment.this);
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                }

                @Override
                public void onDeleteClick(Item item) {
                    SelectionFrame dialog = new SelectionFrame(requireContext());
                    dialog.setSomething(getString(R.string.app_name), getString(R.string.tip_delete_account), new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {

                        }

                        @Override
                        public void confirmClick() {
                            if (TextUtils.equals(item.userId, current)) {
                                AccountSwitchHelper.setInSwitching(null);
                                quit();
                                AsyncUtils.postDelayed(requireActivity(), r -> {
                                    AccountSwitchHelper.removeExistsUser(requireContext(), item.userId);
                                    startActivity(new Intent(requireContext(), LoginActivity.class));
                                    r.finish();
                                }, 200);
                            } else {
                                AccountSwitchHelper.removeExistsUser(requireContext(), item.userId);
                                adapter.remove(item);
                            }
                        }
                    });
                    dialog.show();
                }
            });
            c.uiThread(r -> {
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void quit() {
        requireContext().stopService(new Intent(requireContext(), WindowShowService.class));
        logout();
        UserSp.getInstance(requireContext()).clearUserInfo();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
        coreManager.logout();
        LoginHelper.broadcastLogout(requireContext());
    }

    private void logout() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("deviceKey", "android");

        HttpUtils.get().url(coreManager.getConfig().USER_LOGOUT_DELETE_PUT)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    public void finishSwitching() {
        requireContext().stopService(new Intent(requireContext(), WindowShowService.class));
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
        coreManager.logout();
        LoginHelper.broadcastLogout(requireContext());
        coreManager.setSelf(null);
        requireActivity().finish();
    }

    @SuppressLint("NewApi")
    private void jump() {
        if (getActivity() == null) {
            DialogHelper.dismissProgressDialog();
            return;
        }
        int userStatus = LoginHelper.prepareUser(requireContext(), coreManager);
        Intent intent = new Intent();
        switch (userStatus) {
            case LoginHelper.STATUS_USER_FULL:
            case LoginHelper.STATUS_USER_NO_UPDATE:
            case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
                boolean login = PreferenceUtils.getBoolean(requireContext(), Constants.LOGIN_CONFLICT, false);
                if (login) {// 登录冲突，退出app再次进入，跳转至历史登录界面
                    intent.setClass(requireContext(), LoginHistoryActivity.class);
                } else {
                    intent.setClass(requireContext(), MainActivity.class);
                }
                break;
            case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                intent.setClass(requireContext(), LoginHistoryActivity.class);
                break;
            case LoginHelper.STATUS_NO_USER:
            default:
                DialogHelper.dismissProgressDialog();
                return;// must return
        }
        DialogHelper.dismissProgressDialog();
        startActivity(intent);
        getActivity().finish();
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {
            tvTitle1.setText(getString(R.string.label_account_switch_edit_title_1));
            tvTitle2.setText(getString(R.string.label_account_switch_edit_title_2));
        } else {
            tvTitle1.setText(getString(R.string.label_account_switch_title_1));
            tvTitle2.setText(getString(R.string.label_account_switch_title_2));
        }
        if (adapter == null) {
            // 还没加载成功，
            return;
        }
        adapter.setEditMode(editMode);
    }

    interface OnItemClickListener {
        void onAccountClick(Item item);

        void onAddClick();

        void onDeleteClick(Item item);
    }

    private static class Item {
        String userId;
        String nickname;
        String account;
        boolean checked;
    }

    private static abstract class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class AddViewHolder extends ViewHolder {
        private LinearLayout llAddAccount = itemView.findViewById(R.id.llAddAccount);

        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
            ButtonColorChange.rechargeChange(itemView.getContext(), llAddAccount, R.drawable.account_switch_add_bg);
        }
    }

    private static class AccountViewHolder extends ViewHolder {
        private ImageView ivDelete = itemView.findViewById(R.id.ivDelete);
        private ImageView ivHead = itemView.findViewById(R.id.ivHead);
        private TextView tvName = itemView.findViewById(R.id.tvName);
        private TextView tvAccount = itemView.findViewById(R.id.tvAccount);
        private TextView ivCurrentAccount = itemView.findViewById(R.id.ivCurrentAccount);

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            SkinUtils.Skin skin = SkinUtils.getSkin(itemView.getContext());
            ivCurrentAccount.setTextColor(skin.getAccentColor());
            ivCurrentAccount.setBackgroundTintList(ColorStateList.valueOf(skin.getBrighterAccentColor()));
        }

        public void apply(Item item, boolean editMode, OnItemClickListener onItemClickListener) {
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onAccountClick(item);
                }
            });
            AvatarHelper.getInstance().displayAvatar(item.nickname, item.userId, ivHead, true);
            tvName.setText(item.nickname);
            tvAccount.setText(item.account);
            if (item.checked & !editMode) {
                ivCurrentAccount.setVisibility(View.VISIBLE);
            } else {
                ivCurrentAccount.setVisibility(View.GONE);
            }
            if (editMode) {
                ivDelete.setVisibility(View.VISIBLE);
                ivDelete.setOnClickListener(v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onDeleteClick(item);
                    }
                });
            } else {
                ivDelete.setVisibility(View.GONE);
            }
        }
    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int TYPE_ACCOUNT = 1;
        private static final int TYPE_ADD = 2;
        private List<Item> data;
        private OnItemClickListener onItemClickListener;
        private boolean editMode;

        public Adapter(List<Item> data, boolean editMode, OnItemClickListener onItemClickListener) {
            this.data = data;
            this.editMode = editMode;
            this.onItemClickListener = onItemClickListener;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == data.size()) {
                return TYPE_ADD;
            }
            return TYPE_ACCOUNT;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_ADD) {
                return new AddViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_switch_add, parent, false));
            }
            return new AccountViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_switch, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder pHolder, int position) {
            if (pHolder instanceof AddViewHolder) {
                pHolder.itemView.setOnClickListener(v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onAddClick();
                    }
                });
                return;
            }
            AccountViewHolder holder = (AccountViewHolder) pHolder;
            Item item = data.get(position);
            holder.apply(item, editMode, onItemClickListener);
        }

        @Override
        public int getItemCount() {
            return data.size() + 1;
        }

        public void setEditMode(boolean editMode) {
            this.editMode = editMode;
            notifyDataSetChanged();
        }

        public void remove(Item item) {
            int index = data.indexOf(item);
            data.remove(index);
            notifyItemRemoved(index);
        }
    }

}
