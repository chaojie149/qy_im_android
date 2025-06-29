package com.tongxin.caihong.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Applet;
import com.tongxin.caihong.bean.ConfigBean;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.MessageEventHongdian;
import com.tongxin.caihong.call.CallConstants;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MyZanDao;
import com.tongxin.caihong.helper.AdHelper;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.groupchat.SelectContactsActivity;
import com.tongxin.caihong.ui.life.LifeCircleActivity;
import com.tongxin.caihong.ui.me.NearPersonActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.nearby.NearGroupActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.ad.AdView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.builder.BaseBuilder;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

public class SquareFragment extends EasyFragment {
    private static final String TAG = "SquareFragment";
    private SquareActionAdapter adapter;
    private List<Item> data;
    private PublicAdapter publicAdapter;
    private AdView adView;

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_square;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        ((TextView) findViewById(R.id.tvText)).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        ImageView ivRight = findViewById(R.id.iv_title_right);
        ivRight.setOnClickListener(v -> {
            MainActivity.requestQrCodeScan(getActivity());
        });
        RecyclerView rvAction = findViewById(R.id.rvAction);
        rvAction.setLayoutManager(new GridLayoutManager(requireContext(), 4, LinearLayoutManager.VERTICAL, false));
        data = getData();
        adapter = new SquareActionAdapter(data);
        rvAction.setAdapter(adapter);
        // 避免刷新时的闪烁，
        rvAction.setItemAnimator(null);
        EventBus.getDefault().register(this);

        adView = findViewById(R.id.adView);
        AdHelper.bind(this, adView, AdHelper.AdType.Banner);
        AdHelper.setShowed(requireContext(), AdHelper.AdType.Banner);
        if (adView.isLoaded()) {
            AdHelper.preloadAd(requireContext(), AdHelper.AdType.Banner);
        }

        AsyncUtils.doAsync(this, throwable -> {
            Reporter.post("获取生活圈新消息数量失败，", throwable);
            Activity ctx = getActivity();
            if (ctx != null) {
                ctx.runOnUiThread(() -> ToastUtil.showToast(requireContext(), R.string.tip_get_life_circle_number_failed));
            }
        }, squareFragmentAsyncContext -> {
            final int lifeCircleNumber = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
            squareFragmentAsyncContext.uiThread(squareFragment -> squareFragment.updateLifeCircleNumber(lifeCircleNumber));
        });

        if (!coreManager.getConfig().enableMpModule) {
            findViewById(R.id.llHotNumber).setVisibility(View.GONE);
        } else {
            RecyclerView rvPublicNumber = findViewById(R.id.rvPublicNumber);
            rvPublicNumber.setHasFixedSize(true);
            rvPublicNumber.setNestedScrollingEnabled(false);
            LinearLayoutManager lm = new LinearLayoutManager(requireContext());
            rvPublicNumber.setLayoutManager(lm);
            publicAdapter = new PublicAdapter();
            rvPublicNumber.setAdapter(publicAdapter);
            requestServiceNumber();
        }

        rvAction.post(() -> {
            // post内的方法会等界面完全可见才调用
            // 等界面完全可见在调用此方法，防止红点出来之后adapter还要刷新导致红点又消失的问题
            if (MainActivity.isSquareFragmentCreatedRefresh) {
                MainActivity.isSquareFragmentCreatedRefresh = false;
                refreshLifeCircle(-1);
            }
        });
        initApplet();
    }

    private void initApplet() {
        if (!coreManager.getConfig().enableOpenModule) {
            findViewById(R.id.llApplet).setVisibility(View.GONE);
            return;
        }
//        findViewById(R.id.rlApplet).setOnClickListener(v -> AppletListActivity.start(requireContext()));
        requestApplet();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            adView.pause();
        } else {
            adView.resume();
        }
    }


    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    // 更新发现模块新消息数量
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventHongdian message) {
        refreshLifeCircle(message.number);
    }

    private void refreshLifeCircle(int number) {
        if (number == -1) {
            // 好友更新了动态
            int size = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
            if (size != 0) {
                // 本地社交圈有未读数量，不更新红点
                return;
            }
        }
        updateLifeCircleNumber(number);
    }

    private void updateLifeCircleNumber(int number) {
        for (int i = 0; i < data.size(); i++) {
            Item item = data.get(i);
            if (item instanceof LocalItem && ((LocalItem) item).textRes == R.string.life_circle) {
                ((LocalItem) item).number = number;
                adapter.notifyItemChanged(i);
                return;
            }
        }
    }

    private List<Item> getData() {
        List<Item> ret = new LinkedList<>();
        // lifeCircle  生活圈，  videoMeeting 视频会议，  liveVideo 视频直播，  shortVideo 短视频， peopleNearby 附近的人
        ConfigBean.PopularApp popularAPP = coreManager.getConfig().popularAPP;
        if (popularAPP.lifeCircle > 0) {
            ret.add(new LocalItem(R.string.life_circle, R.mipmap.square_item_life, toStartActivity(LifeCircleActivity.class)));
        }

        if (popularAPP.videoMeeting > 0) {
            ret.add(new LocalItem(R.string.chat_video_conference, R.mipmap.square_item_video_meeting, () -> SelectContactsActivity.startQuicklyInitiateMeeting(requireContext(), CallConstants.Video_Meet)));
        }


//        if (popularAPP.liveVideo > 0) {
//            ret.add(new LocalItem(R.string.live_chat, R.mipmap.square_item_live_chat,toStartActivity(LiveActivity.class)));
//        }

      /*  if (popularAPP.shortVideo > 0) {
            ret.add(new LocalItem(R.string.douyin, R.mipmap.square_item_douyin, toStartActivity(TrillActivity.class)));
        }
*/

        if (popularAPP.peopleNearby > 0) {
            ret.add(new LocalItem(R.string.near_person, R.mipmap.square_item_nearby, toStartActivity(NearPersonActivity.class)));
            ret.add(new LocalItem(R.string.near_group, R.mipmap.square_item_nearby_group, toStartActivity(NearGroupActivity.class)));
        }
        return new ArrayList<>(ret);
    }

    private Runnable toStartActivity(final Class<? extends Activity> clazz) {
        return () -> {
            Intent intent = new Intent(requireContext(), clazz);
            startActivity(intent);
        };
    }

    @SuppressWarnings("unused")
    private Runnable toToast() {
        return () -> ToastUtil.showToast(requireContext(), R.string.tip_coming_soon);
    }

    public void requestApplet() {
        Map<String, String> params = new HashMap<>();
        params.put("pageIndex", String.valueOf(0));
        params.put("pageSize", String.valueOf(999));

        BaseBuilder.BaseCall build = HttpUtils.get().url(coreManager.getConfig().FX_LIST)
                .params(params)
                .build();
        build.execute(new ListCallback<Applet>(Applet.class) {

            @Override
            public void onResponse(ArrayResult<Applet> result) {
                if (getContext() == null) {
                    // 防崩溃，
                    return;
                }
                if (Result.checkSuccess(requireContext(), result)) {
                    List<Applet> data = result.getData();
                    addAppletList(data);
                }
            }

            @Override
            public void onError(Call call, Exception e) {
                if (getContext() == null) {
                    // 防崩溃，
                    return;
                }
                ToastUtil.showNetError(requireContext());
            }
        });
    }

    private void addAppletList(List<Applet> appletList) {
//        if (appletList!=null&&appletList.size()>0) {
//            CoreManager.getInstance(getContext()).getConfig().homeAddress.setHomeUrl(appletList.get(0).getUrl());
//            WebViewFragment webViewFragment = new WebViewFragment();
////            webViewFragment.setCloseEvent(new WebViewFragment.CloseEvent() {
////                @Override
////                public void close() {
////                    findViewById(R.id.fl).setVisibility(View.GONE);
////                }
////            });
//            SquareFragment.this.getChildFragmentManager().beginTransaction().replace(R.id.fl, webViewFragment).commitAllowingStateLoss();
//            findViewById(R.id.fl).setVisibility(View.VISIBLE);
//        }
//        findViewById(R.id.fl).setVisibility(View.VISIBLE);
        RecyclerView rvApplet = findViewById(R.id.rvApplet);
        rvApplet.setLayoutManager(new GridLayoutManager(requireContext(), 1, LinearLayoutManager.VERTICAL, false));
        List<Item> data = new ArrayList<>(appletList.size());
        for (Applet applet : appletList) {
            data.add(new AppletItem(applet));
        }
        rvApplet.setAdapter(new SquareActionAdapter1(data));
    }

    private void requestServiceNumber() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("page", String.valueOf(0));
        params.put("limit", String.valueOf(AppConfig.PAGE_SIZE));

        DialogHelper.showDefaulteMessageProgressDialogAddCancel(requireActivity(), null);

        HttpUtils.get().url(coreManager.getConfig().PUBLIC_SEARCH)
                .params(params)
                .build()
                .execute(new ListCallback<User>(User.class) {
                    @Override
                    public void onResponse(ArrayResult<User> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(getContext(), result)) {
                            List<User> list = result.getData();
                            publicAdapter.setData(list);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(getContext());
                    }
                });
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private final View llRoot;
        private final TextView tvActionName;
        private final ImageView ivActionImage;
        private final TextView tvNumber;

        public ViewHolder(View itemView) {
            super(itemView);
            llRoot = itemView.findViewById(R.id.llRoot);
            tvActionName = itemView.findViewById(R.id.tvActionName);
            ivActionImage = itemView.findViewById(R.id.ivActionImage);
            tvNumber = itemView.findViewById(R.id.tvNumber);

            // ImageViewCompat.setImageTintList(ivActionImage, SkinUtils.getSkin(itemView.getContext()).getButtonColorState());
        }
    }

    private abstract static class Item {
        public abstract void showText(TextView textView);

        public abstract void showImage(ImageView imageView);

        public abstract void onClick(View v);

        // 小红点提示的数量，
        // 0就不显示小红点，
        public abstract int getNumber();
    }

    private static class LocalItem extends Item {
        private final int textRes;
        private final Runnable onClickCallback;
        private int imageRes;
        private int number = 0;

        LocalItem(@StringRes int textRes, @DrawableRes int imageRes, Runnable onClickCallback) {
            this(textRes, imageRes, onClickCallback, 0);
        }

        LocalItem(@StringRes int textRes, @DrawableRes int imageRes, Runnable onClickCallback, int number) {
            this.textRes = textRes;
            this.imageRes = imageRes;
            this.onClickCallback = onClickCallback;
            this.number = number;
        }

        @Override
        public void showText(TextView textView) {
            textView.setText(textRes);
        }

        @Override
        public void showImage(ImageView imageView) {
            imageView.setImageResource(imageRes);
        }

        @Override
        public void onClick(View v) {
            onClickCallback.run();
        }

        @Override
        public int getNumber() {
            return number;
        }
    }

    private class AppletItem extends Item {
        private Applet applet;

        private AppletItem(Applet applet) {
            this.applet = applet;
        }

        @Override
        public void showText(TextView textView) {
            textView.setText(applet.getName());
        }

        @Override
        public void showImage(ImageView imageView) {
            AvatarHelper.getInstance().displayUrl(applet.getIcon(), imageView);
        }

        @Override
        public void onClick(View v) {
            CoreManager.getInstance(v.getContext()).getConfig().homeAddress.setHomeUrl(applet.getUrl());
            WebViewFragment webViewFragment = new WebViewFragment();
            webViewFragment.setCloseEvent(new WebViewFragment.CloseEvent() {
                @Override
                public void close() {
                    findViewById(R.id.fl).setVisibility(View.GONE);
                }
            });
            SquareFragment.this.getChildFragmentManager().beginTransaction().replace(R.id.fl,webViewFragment ).commitAllowingStateLoss();
            findViewById(R.id.fl).setVisibility(View.VISIBLE);
//            AppletActivity.start(v.getContext(), applet.getUrl());
        }

        @Override
        public int getNumber() {
            return 0;
        }
    }

    private static class PublicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead = itemView.findViewById(R.id.notice_iv);
        TextView tvName = itemView.findViewById(R.id.notice_tv);
        TextView tvDesc = itemView.findViewById(R.id.desc_tv);

        PublicViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class SquareActionAdapter1 extends RecyclerView.Adapter<ViewHolder> {
        private final List<Item> data;

        private SquareActionAdapter1(List<Item> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_square_action1, parent, false);
            final ViewHolder vh = new ViewHolder(itemView);
//            int width = ScreenUtil.getScreenWidth(parent.getContext());
//            ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
////            layoutParams.width = width / 5;
//            layoutParams.width = width;
//            vh.itemView.setLayoutParams(layoutParams);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Item item = data.get(position);
            holder.itemView.setOnClickListener(v -> {
                if (UiUtils.isNormalClick(v)) {
                    item.onClick(v);
                }
            });
            item.showImage(holder.ivActionImage);
            item.showText(holder.tvActionName);
            UiUtils.updateNum(holder.tvNumber, item.getNumber());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static class SquareActionAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<Item> data;

        private SquareActionAdapter(List<Item> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_square_action, parent, false);
            final ViewHolder vh = new ViewHolder(itemView);
            int width = ScreenUtil.getScreenWidth(parent.getContext());
            ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
            layoutParams.width = width / 5;
            vh.itemView.setLayoutParams(layoutParams);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Item item = data.get(position);
            holder.itemView.setOnClickListener(v -> {
                if (UiUtils.isNormalClick(v)) {
                    item.onClick(v);
                }
            });
            item.showImage(holder.ivActionImage);
            item.showText(holder.tvActionName);
            UiUtils.updateNum(holder.tvNumber, item.getNumber());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class PublicAdapter extends RecyclerView.Adapter<PublicViewHolder> {
        private List<User> data = Collections.emptyList();

        public void setData(List<User> data) {
            this.data = new ArrayList<>(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PublicViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new PublicViewHolder(getLayoutInflater().inflate(R.layout.item_square_public_number, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PublicViewHolder vh, int i) {
            User item = data.get(i);
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), item.getUserId());
            if (friend != null) {
                vh.tvName.setText(TextUtils.isEmpty(friend.getRemarkName()) ? item.getNickName() : friend.getRemarkName());
            } else {
                vh.tvName.setText(item.getNickName());
            }
            if (TextUtils.isEmpty(item.getDescription())) {
                vh.tvDesc.setVisibility(View.GONE);
            } else {
                vh.tvDesc.setVisibility(View.VISIBLE);
                vh.tvDesc.setText(item.getDescription());
            }
            AvatarHelper.getInstance().displayAvatar(item.getNickName(), item.getUserId(), vh.ivHead, true);

            vh.itemView.setOnClickListener(v -> {
                Friend friend2 = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), item.getUserId());
                if (friend2 != null && (friend2.getStatus() == Friend.STATUS_FRIEND || friend2.getStatus() == Friend.STATUS_SYSTEM)) {
                    ChatActivity.start(requireContext(), friend2);
                } else {
                    BasicInfoActivity.start(requireContext(), item.getUserId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
