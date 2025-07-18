package com.tongxin.caihong.ui.contacts;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Contact;
import com.tongxin.caihong.bean.Contacts;
import com.tongxin.caihong.db.dao.ContactDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ContactsUtil;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.PullToRefreshSlideListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SendContactsActivity extends BaseActivity {
    private SideBar mSideBar;
    private TextView mTextDialog;
    private PullToRefreshSlideListView mListView;
    private ContactsAdapter mContactsAdapter;
    private List<Contacts> mContactList;
    private List<BaseSortModel<Contacts>> mSortContactList;
    private BaseComparator<Contacts> mBaseComparator;
    private String mLoginUserId;
    // 全选
    private TextView tvTitleRight;
    private boolean isBatch;
    private Map<String, Contacts> mBatchAddContacts = new HashMap<>();
    private TextView mBatchAddTv;

    private Map<String, Contacts> phoneContacts;

    private String mobilePrefix;

    public static void start(Activity ctx, int requestCode) {
        if (!PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) ctx, Short.MAX_VALUE, () -> {
            start(ctx, requestCode);
        }, null, PermissionUtil.getReadContactsPermissions())) {
            return;
        }
        Intent intent = new Intent(ctx, SendContactsActivity.class);
        ctx.startActivityForResult(intent, requestCode);
    }

    private static void makeResult(Intent intent, List<Contacts> contactsList) {
        intent.putExtra("contactsList", JSON.toJSONString(contactsList));
    }

    @Nullable
    public static List<Contacts> parseResult(Intent intent) {
        if (intent == null) {
            return null;
        }
        String str = intent.getStringExtra("contactsList");
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            return JSON.parseArray(str, Contacts.class);
        } catch (Exception e) {
            // 以防万一，无论如何不能在这崩溃，
            Reporter.unreachable(e);
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_msg_invite);

        mLoginUserId = coreManager.getSelf().getUserId();
        mContactList = new ArrayList<>();
        mSortContactList = new ArrayList<>();
        mBaseComparator = new BaseComparator<>();
        mContactsAdapter = new ContactsAdapter();

        mobilePrefix = PreferenceUtils.getString(MyApplication.getContext(), Constants.AREA_CODE_KEY);

        initActionBar();
        initView();
        boolean isReadContacts = PermissionUtil.checkSelfPermissions(this, new String[]{Manifest.permission.READ_CONTACTS});
        if (!isReadContacts) {
            DialogHelper.tip(this, "请开启通讯录权限");
            return;
        }

        dataLayering();
        initEvent();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.phone_contact));

        tvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        tvTitleRight.setText(getString(R.string.select_all));
    }

    public void initView() {
        mListView = (PullToRefreshSlideListView) findViewById(R.id.pull_refresh_list);
        mListView.getRefreshableView().setAdapter(mContactsAdapter);
        mListView.setMode(PullToRefreshBase.Mode.DISABLED);

        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mContactsAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListView.getRefreshableView().setSelection(position);
                }
            }
        });

        mBatchAddTv = (TextView) findViewById(R.id.sure_add_tv);
        ButtonColorChange.textChange(mContext, mBatchAddTv);
    }

    private void dataLayering() {
        phoneContacts = ContactsUtil.getPhoneContacts(this);

        List<Contact> allContacts = ContactDao.getInstance().getAllContacts(mLoginUserId);
        // 现在数据库内在创建联系人的时候已经去重了，按理说这里已经不需要处理了，但是一些老用户联系人表内已经生成了一些重复的数据，所以这里在去下重
        Set<Contact> set = new TreeSet<>(new Comparator<Contact>() {
            @Override
            public int compare(Contact o1, Contact o2) {
                return o1.getToUserId().compareTo(o2.getToUserId());
            }
        });
        set.addAll(allContacts);
        allContacts = new ArrayList<>(set);

        // 移除已注册IM的联系人，显示未注册IM的联系人
        for (int i = 0; i < allContacts.size(); i++) {
            phoneContacts.remove(allContacts.get(i).getTelephone());
        }

        Collection<Contacts> values = phoneContacts.values();
        mContactList = new ArrayList<>(values);

        DialogHelper.showDefaulteMessageProgressDialog(this);
        try {
            AsyncUtils.doAsync(this, e -> {
                Reporter.post("加载数据失败，", e);
                AsyncUtils.runOnUiThread(this, ctx -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(ctx, R.string.data_exception);
                });
            }, c -> {
                Map<String, Integer> existMap = new HashMap<>();
                List<BaseSortModel<Contacts>> sortedList = SortHelper.toSortedModelList(mContactList, existMap, Contacts::getName);
                c.uiThread(r -> {
                    DialogHelper.dismissProgressDialog();
                    mSideBar.setExistMap(existMap);
                    mSortContactList = sortedList;
                    mContactsAdapter.setData(sortedList);
                });
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initEvent() {
        tvTitleRight.setOnClickListener(v -> isControlBatchStatus(true));

        TextView shareAppTv = findViewById(R.id.share_app_tv);
        shareAppTv.setText(getString(R.string.share_app, getString(R.string.app_name)));
        findViewById(R.id.invited_friend_ll).setVisibility(View.GONE);

        mListView.getRefreshableView().setOnItemClickListener((parent, view, position, id) -> {
            position = (int) id;
            Contacts contact = mSortContactList.get(position).getBean();
            if (contact != null) {
                if (mBatchAddContacts.containsKey(contact.getTelephone())) {
                    mBatchAddContacts.remove(contact.getTelephone());
                    isControlBatchStatus(false);
                } else {
                    mBatchAddContacts.put(contact.getTelephone(), contact);
                }
                mContactsAdapter.notifyDataSetChanged();
            }
        });

        mBatchAddTv.setOnClickListener(v -> {
            Collection<Contacts> values = mBatchAddContacts.values();
            List<Contacts> contactList = new ArrayList<>(values);
            if (contactList.size() == 0) {
                return;
            }
            String telStr = JSON.toJSONString(contactList);
            sendContacts(contactList);
        });
    }

    private void isControlBatchStatus(boolean isChangeAll) {
        if (isChangeAll) {
            isBatch = !isBatch;
            if (isBatch) {
                tvTitleRight.setText(getString(R.string.cancel));
                for (int i = 0; i < mContactList.size(); i++) {
                    mBatchAddContacts.put(mContactList.get(i).getTelephone(), mContactList.get(i));
                }
            } else {
                tvTitleRight.setText(getString(R.string.select_all));
                mBatchAddContacts.clear();
            }
            mContactsAdapter.notifyDataSetChanged();
        } else {
            isBatch = false;
            tvTitleRight.setText(getString(R.string.select_all));
        }
    }

    private void sendContacts(List<Contacts> contactList) {
        Intent intent = new Intent();
        makeResult(intent, contactList);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    class ContactsAdapter extends BaseAdapter implements SectionIndexer {
        List<BaseSortModel<Contacts>> mSortContactList;

        public ContactsAdapter() {
            mSortContactList = new ArrayList<>();
        }

        public void setData(List<BaseSortModel<Contacts>> sortContactList) {
            mSortContactList = sortContactList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSortContactList.size();
        }

        @Override
        public Object getItem(int position) {
            return mSortContactList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_contacts_msg_invite, parent, false);
            }
            TextView categoryTitleTv = ViewHolder.get(convertView, R.id.catagory_title);
            View view_bg_friend = ViewHolder.get(convertView, R.id.view_bg_friend);
            CheckBox checkBox = ViewHolder.get(convertView, R.id.check_box);
            ButtonColorChange.tintCheckBox(checkBox);
            ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
            TextView contactNameTv = ViewHolder.get(convertView, R.id.contact_name_tv);
            TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);

            // 根据position获取分类的首字母的Char ascii值
            int section = getSectionForPosition(position);
            // 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == getPositionForSection(section)) {
                categoryTitleTv.setVisibility(View.VISIBLE);
                categoryTitleTv.setText(mSortContactList.get(position).getFirstLetter());
                view_bg_friend.setVisibility(View.GONE);
            } else {
                categoryTitleTv.setVisibility(View.GONE);
                view_bg_friend.setVisibility(View.VISIBLE);
            }

            final Contacts contact = mSortContactList.get(position).getBean();
            if (contact != null) {
                checkBox.setChecked(mBatchAddContacts.containsKey(contact.getTelephone()));
                AvatarHelper.getInstance().displayAddressAvatar(contact.getName(), avatarImg);
                contactNameTv.setText(contact.getName());
                // 因为存储的时候默认拼上了区号，这里将区号截掉显示
                String tel = contact.getTelephone().substring(String.valueOf(mobilePrefix).length());
                userNameTv.setText(tel);
            }

            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int section) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortContactList.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == section) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortContactList.get(position).getFirstLetter().charAt(0);
        }
    }
}
