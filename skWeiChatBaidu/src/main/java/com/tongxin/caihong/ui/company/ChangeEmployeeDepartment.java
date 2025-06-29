package com.tongxin.caihong.ui.company;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.company.StructBeanNetInfo;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
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

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

/**
 * 更换员工部门
 * 选择部门
 */
public class ChangeEmployeeDepartment extends BaseActivity {
    public static final int REQUEST_SELECT_DEPARTMENT = 0x01;
    private DepartmentAdapter adapter;
    private List<Item> data = new ArrayList<>();
    private String companyId;
    private String userId;
    // 是否更换部门，false：选择部门，部门数据从服务器获取
    private boolean isChange;

    public static void start(Activity ctx, String companyId) {
        Intent intent = new Intent(ctx, ChangeEmployeeDepartment.class);
        intent.putExtra("companyId", companyId);
        intent.putExtra("isChange", false);
        ctx.startActivityForResult(intent, REQUEST_SELECT_DEPARTMENT);
    }

    public static void start(Context ctx, String companyId, String userId, String departmentIdList, String departmentNameList) {
        Intent intent = new Intent(ctx, ChangeEmployeeDepartment.class);
        intent.putExtra("companyId", companyId);
        intent.putExtra("userId", userId);
        intent.putExtra("departmentIdList", departmentIdList);
        intent.putExtra("departmentNameList", departmentNameList);
        intent.putExtra("isChange", true);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_employee_department);
        companyId = getIntent().getStringExtra("companyId");
        userId = getIntent().getStringExtra("userId");
        isChange = getIntent().getBooleanExtra("isChange", false);
        if (isChange) {
            // 更换部门
            List<String> departmentIdList = JSON.parseArray(getIntent().getStringExtra("departmentIdList"), String.class);
            List<String> departmentNameList = JSON.parseArray(getIntent().getStringExtra("departmentNameList"), String.class);
            if (departmentIdList == null) {
                departmentIdList = new ArrayList<>();
            }
            if (departmentNameList == null) {
                departmentNameList = new ArrayList<>();
            }
            for (int i = 0; i < departmentIdList.size(); i++) {
                data.add(new Item(departmentIdList.get(i), departmentNameList.get(i)));
            }
        } else {
            // 选择部门，部门数据从服务器获取
            loadData();
        }
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(isChange ? getString(R.string.change_departement) : getString(R.string.select_department));
    }

    private void initView() {
        ListView lvDepartment = findViewById(R.id.lvDepartment);
        adapter = new DepartmentAdapter(this);
        lvDepartment.setAdapter(adapter);
        lvDepartment.setOnItemClickListener((adapterView, view, i, l) -> {
            Item item = (Item) adapter.getItem(i);
            if (isChange) {
                changeEmployeeDepartment(item.getId());
            } else {
                Intent intent = new Intent();
                intent.putExtra("departmentId", item.getId());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void loadData() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("companyId", companyId);

        HttpUtils.get().url(coreManager.getConfig().ORG_COMPANY_DEPARTMENT_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<StructBeanNetInfo.DepartmentsBean>(StructBeanNetInfo.DepartmentsBean.class) {

                    @Override
                    public void onResponse(ArrayResult<StructBeanNetInfo.DepartmentsBean> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            List<StructBeanNetInfo.DepartmentsBean> departmentsBeans = result.getData();
                            if (departmentsBeans == null) {
                                departmentsBeans = new ArrayList<>();
                            }
                            for (int i = 0; i < departmentsBeans.size(); i++) {
                                if (i != 0) {
                                    // 第一个部门为根部门，不允许被选择
                                    data.add(new Item(departmentsBeans.get(i).getId(), departmentsBeans.get(i).getDepartName()));
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void changeEmployeeDepartment(String newDepartmentId) {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("userId", userId);
        params.put("newDepartmentId", newDepartmentId);

        HttpUtils.get().url(coreManager.getConfig().MODIFY_EMPLOYEE_DEPARTMENT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(ChangeEmployeeDepartment.this, R.string.change_departement_succ);
                            // 更换部门成功,跳转至公司管理页面
                            EventBus.getDefault().post(new MessageEvent("Update"));// 数据有更新
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private class DepartmentAdapter extends BaseAdapter {
        private Context ctx;

        public DepartmentAdapter(Context context) {
            ctx = context;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(ctx).inflate(R.layout.a_item_for_change_department, viewGroup, false);
            }
            TextView tvName = ViewHolder.get(view, R.id.tvName);
            tvName.setText(data.get(i).getName());
            return view;
        }
    }

    class Item {
        String id;
        String name;

        public Item() {
        }

        public Item(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
