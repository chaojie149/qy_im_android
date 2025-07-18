package com.tongxin.caihong.call.talk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.view.TalkUserDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TalkUserAdapter extends RecyclerView.Adapter<TalkUserAdapter.ViewHolder> {
    private static final String TAG = "TalkUserAdapter";
    private static final int TALKING_LENGTH_UPDATE = 592;
    private Context ctx;
    private List<Item> data;
    @Nullable
    private Talking talking;
    private TalkingLengthHandler talkingLengthHandler = new TalkingLengthHandler(this);

    public TalkUserAdapter(Context ctx) {
        this.ctx = ctx;
        data = new ArrayList<>();
        setHasStableIds(true);
    }

    public void setData(List<Item> data) {
        this.data = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void add(Item item) {
        int index = data.size();
        if (data.contains(item)) {
            // 已经在列表中了，
            return;
        }
        if (talking != null && TextUtils.equals(item.userId, talking.userId)) {
            // 加入需要时间，所以有可能加入前已经发送过占线xmpp,
            item.talking = true;
            item.requestTime = talking.requestTime;
        }
        data.add(item);
        notifyItemInserted(index);
    }

    public void remove(Item item) {
        int index = data.indexOf(item);
        if (index < 0) {
            // 没找到，
            return;
        }
        data.remove(index);
        notifyItemRemoved(index);
    }

    public void updateTalking(@Nullable Talking talking) {
        if (Objects.equals(this.talking, talking)) {
            return;
        }
        Talking oldTalking = this.talking;
        this.talking = talking;
        if (oldTalking != null) {
            int oldIndex = indexOf(oldTalking.userId);
            if (oldIndex != -1) {
                Item item = data.get(oldIndex);
                item.talking = false;
                item.talkLength = oldTalking.talkLength;
                notifyItemChanged(oldIndex);
            }
        }
        if (talking != null) {
            int index = indexOf(talking.userId);
            if (index != -1) {
                Item item = data.remove(index);
                item.talking = true;
                item.requestTime = talking.requestTime;
                item.talkLength = 0L;
                // 正在说话的置顶，
                data.add(0, item);
                notifyItemChanged(index);
                notifyItemMoved(index, 0);
                talkingLengthHandler.sendEmptyMessageDelayed(TALKING_LENGTH_UPDATE, TimeUnit.SECONDS.toMillis(1));
            }
        }
    }

    public void offline(Talking talking) {
        if (Objects.equals(this.talking, talking)) {
            this.talking = null;
        }
        int index = indexOf(talking.userId);
        if (index != -1) {
            data.remove(index);
            notifyItemRemoved(index);
        }
    }

    private void updateTalkingLength() {
        if (talking == null) {
            // handler循环更新在这里结束，不发message就结束了，
            return;
        }
        int index = indexOf(talking.userId);
        if (index != -1) {
            Item item = data.get(index);
            item.talkLength = TimeUtils.sk_time_current_time() - talking.requestTime;
            notifyItemChanged(index);
            talkingLengthHandler.sendEmptyMessageDelayed(TALKING_LENGTH_UPDATE, TimeUnit.SECONDS.toMillis(1));
        }
    }

    private int indexOf(String userId) {
        for (int i = 0; i < data.size(); i++) {
            if (TextUtils.equals(data.get(i).userId, userId)) {
                return i;
            }
        }
        Log.w(TAG, "indexOf: 找不到用户， userId = " + userId, new Exception());
        return -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(ctx).inflate(R.layout.jitsi_talk_item, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Item item = data.get(i);
        viewHolder.apply(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public long getItemId(int position) {
        return Long.valueOf(data.get(position).userId);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @SuppressLint("SimpleDateFormat")
        private ImageView ivHead = itemView.findViewById(R.id.ivHead);
        private TextView tvName = itemView.findViewById(R.id.tvName);

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void apply(Item item) {
            itemView.setOnClickListener(v -> {
                TalkUserDialog.show(itemView.getContext(), item);
            });
            AvatarHelper.getInstance().displayAvatar(item.name, item.userId, ivHead, true);
            tvName.setText(item.name);
/*
            if (item.talking) {
                ivTalking.setVisibility(View.VISIBLE);
            } else {
                ivTalking.setVisibility(View.GONE);
            }
            if (item.requestTime == 0) {
                tvLastRequest.setText("");
            } else {
                tvLastRequest.setText(sdfTime.format(new Date((long) (item.requestTime))));
            }
            if (item.talkLength == 0) {
                tvTalkLength.setText("");
            } else {
                long duration = (long) (double) (item.talkLength / 1000);
                tvTalkLength.setText(String.format(Locale.CHINA, "%02d:%02d:%02d",
                        duration / 3600, (duration % 3600) / 60, (duration % 60)));
            }
*/
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class Item {
        public String name;
        @NonNull
        public String userId;
        public boolean talking = false;
        // 上次抢麦的时间，
        public long requestTime;
        public long talkLength;

        @NonNull
        public static Item fromUser(User user) {
            Item item = new Item();
            item.name = user.getNickName();
            item.userId = user.getUserId();
            return item;
        }

        @NonNull
        public static Item fromMessage(ChatMessage chatMessage) {
            Item item = new Item();
            item.name = chatMessage.getFromUserName();
            item.userId = chatMessage.getFromUserId();
            return item;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Item && Objects.equals(userId, ((Item) obj).userId);
        }

        @Override
        public String toString() {
            return "Item{" +
                    "name='" + name + '\'' +
                    ", userId='" + userId + '\'' +
                    ", talking=" + talking +
                    ", requestTime=" + requestTime +
                    ", talkLength=" + talkLength +
                    '}';
        }
    }

    private static class TalkingLengthHandler extends Handler {
        private WeakReference<TalkUserAdapter> weakRef;

        private TalkingLengthHandler(TalkUserAdapter talkUserAdapter) {
            weakRef = new WeakReference<>(talkUserAdapter);
        }

        @Override
        public void handleMessage(Message msg) {
            TalkUserAdapter adapter = weakRef.get();
            if (adapter == null) {
                return;
            }
            if (msg.what == TALKING_LENGTH_UPDATE) {
                adapter.updateTalkingLength();
            }
        }


    }
}
