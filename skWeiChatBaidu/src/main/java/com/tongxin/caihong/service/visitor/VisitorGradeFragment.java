package com.tongxin.caihong.service.visitor;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.willy.ratingbar.BaseRatingBar;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;

import okhttp3.Call;

public class VisitorGradeFragment extends EasyFragment {
    private String packetId;
    private String friendId;
    private String toId;
    private TextView etContent;
    private TextView tvStar;
    private BaseRatingBar rbStart;

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_service_visitor_grade;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        initView();
        loadData();
    }

    private void loadData() {
        packetId = getArguments().getString("packetId");
        friendId = getArguments().getString("friendId");
        toId = getArguments().getString("toId");
    }

    private void initView() {
        tvStar = findViewById(R.id.tvStar);
        rbStart = findViewById(R.id.rbStart);
        String[] stringArray = getResources().getStringArray(R.array.service_grade_star);
        rbStart.setOnRatingChangeListener((ratingBar, rating, fromUser) -> {
            tvStar.setText(stringArray[((int) rating) - 1]);
        });
        tvStar.setText(stringArray[((int) rbStart.getRating()) - 1]);
        etContent = findViewById(R.id.etContent);
        Button btnCommit = findViewById(R.id.btnCommit);
        ButtonColorChange.colorChange(requireContext(), btnCommit);
        btnCommit.setOnClickListener(v -> {
            commit();
        });
    }

    private void commit() {
        int star = (int) rbStart.getRating();
        String content = etContent.getText().toString();
        HashMap<String, String> params = new HashMap<>();
        params.put("companyMpId", friendId);
        params.put("grade", String.valueOf(star));
        params.put("content", content);

        HttpUtils.get().url(CoreManager.requireConfig(requireContext()).SERVICE_GRADE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(requireContext(), result)) {
                            ToastUtil.showToast(requireContext(), R.string.service_grade_success);
                            VisitorGradeActivity.sLastGradeTime = System.currentTimeMillis();
                            if (!TextUtils.isEmpty(packetId)) {
                                ChatMessageDao.getInstance().updateServiceGradeMessageStatus(coreManager.getSelf().getUserId(), friendId, packetId, true);
                            }
                            if (getActivity() instanceof OnSuccessListener) {
                                ((OnSuccessListener) getActivity()).onSuccess();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(requireContext());
                    }
                });
    }

    interface OnSuccessListener {
        void onSuccess();
    }
}
