package com.tongxin.caihong.helper;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.event.EventSendVerifyMsg;
import com.tongxin.caihong.ui.dialog.SingleInputDialogView;
import com.tongxin.caihong.ui.dialog.TowInputDialogView;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.CustomizeProgressDialog;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.view.VerifyDialog;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

/**
 * 只有一个确定按钮的使用DialogHelper.tip,
 * 有确定和取消的用SelectionFrame,
 *
 * @编写人： TanX
 * @时间： 2016/5/3 12:30
 * @说明：
 * @功能： 统一管理dialog
 **/
public class DialogHelper {

    /*
    InputFilter
     */
    public static InputFilter mExpressionFilter = new InputFilter() {
        Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
                Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Matcher emojiMatcher = emoji.matcher(source);
            if (emojiMatcher.find()) {
                Toast.makeText(MyApplication.getContext(), R.string.tip_not_support_emoji, Toast.LENGTH_SHORT).show();
                return "";
            }
            return null;
        }
    };
    public static InputFilter mChineseEnglishNumberFilter = new InputFilter() {
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\u4E00-\\u9FA5_]");

        @Override
        public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
            Matcher matcher = pattern.matcher(charSequence);
            if (!matcher.find()) {
                return null;
            } else {
                Toast.makeText(MyApplication.getContext(), R.string.tip_chinese_english_number, Toast.LENGTH_SHORT).
                        show();
                return "";
            }
        }
    };
    // private static ProgressDialog dialog;
    private static CustomizeProgressDialog dialog;

    /**
     * 显示一个输入框提示的dialog,所有属性为必填
     *
     * @param activity
     * @param title             标题
     * @param text              内容
     * @param hint              edittext的hint
     * @param onConfirmListener 确定按钮的点击事件
     */
    public static SingleInputDialogView showLimitSingleInputDialog(Activity activity, String title, String text, String hint, SingleInputDialogView.OnConfirmListener onConfirmListener) {
        return (SingleInputDialogView) new SingleInputDialogView(activity, title, text, hint, onConfirmListener).show();
    }

    /**
     * 显示一个输入框提示的dialog,所有属性为必填
     *
     * @param activity
     * @param title             标题
     * @param text              内容
     * @param hint              edittext的hint
     * @param onConfirmListener 确定按钮的点击事件
     */
    public static SingleInputDialogView showLimitSingleInputDialog(Activity activity, String title, String text, String hint, int maxLength, SingleInputDialogView.OnConfirmListener onConfirmListener) {
        return (SingleInputDialogView) new SingleInputDialogView(activity, title, text, hint, new InputFilter[]{new InputFilter.LengthFilter(maxLength)}, onConfirmListener).show();
    }


    /**
     * 显示一个输入框提示的dialog,所有属性为必填
     *
     * @param activity
     * @param title             标题
     * @param text              内容
     * @param hint              edittext的hint
     * @param onConfirmListener 确定按钮的点击事件
     */
    public static SingleInputDialogView showLimitSingleInputDialog(Activity activity, String title, String text, String hint, InputFilter[] filters, SingleInputDialogView.OnConfirmListener onConfirmListener) {
        return (SingleInputDialogView) new SingleInputDialogView(activity, title, text, hint, filters, onConfirmListener).show();
    }

    /**
     * 显示两个输入框的dialog,并返回该dialog
     *
     * @param activity
     * @param title           标题
     * @param hint            第一个edittext的hint
     * @param hint2           第二个edittext的hint
     * @param onClickListener 确定按钮的点击事件
     * @return 显示的dialog
     */
    public static TowInputDialogView showTowInputDialogAndReturnDialog(Activity activity, String title, String hint, String hint2, TowInputDialogView.onSureClickLinsenter onClickListener) {
        return (TowInputDialogView) new TowInputDialogView(activity, title, hint, hint2, onClickListener).show();
    }

    /**
     * 显示两个输入框的dialog,并给EditText设值,并返回该dialog
     *
     * @param activity
     * @param title           标题
     * @param hint            第一个edittext的hint
     * @param hint2           第二个edittext的hint
     * @param text            第一个edittext的text
     * @param text2           第二个edittext的text
     * @param onClickListener 确定按钮的点击事件
     * @return 显示的dialog
     */
    public static TowInputDialogView showTowInputDialogAndReturnDialog(Activity activity, String title, String hint, String hint2, String text, String text2, TowInputDialogView.onSureClickLinsenter onClickListener) {
        return (TowInputDialogView) new TowInputDialogView(activity, title, hint, hint2, text, text2, onClickListener).show();
    }

    @NonNull
    private static CustomizeProgressDialog createProgressDialog(Activity activity) {
        // 这对话框不能出现多个，以免没能全部关闭，
        // 如果是多个activity都弹这个对话框还是可能有问题，本就不该有全局对话框，
        // TODO: 可以考虑弱引用不同activity创建不同dialog, 相同activity复用dialog, 但是dismiss也要传入activity,
        dismissProgressDialog();
        return new CustomizeProgressDialog(activity);
    }

    private static void safelyShow() {
        try {
            Context ctx = dialog.getContext();
            if (ctx instanceof Activity) {
                Activity act = (Activity) ctx;
                if (act.isFinishing() || act.isDestroyed()) {
                    return;
                }
            }
            dialog.show();
        } catch (Exception e) {
            // 无论如何不能因为这个抛异常，
            Reporter.unreachable(e);
        }
    }

    /**
     * 显示提示message的dialog
     */
    public static void showMessageProgressDialog(Activity activity, String message) {
        dialog = createProgressDialog(activity);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        safelyShow();
    }

    /**
     * 显示提示等待的dialog
     */
    public static void showDefaulteMessageProgressDialog(Activity activity) {
        showMessageProgressDialog(activity, activity.getString(R.string.please_wait));
    }

    /**
     * 显示提示message的dialog
     */
    public static void showMessageProgressDialogAddCancel(Activity activity, String message, DialogInterface.OnCancelListener listener) {
        dialog = createProgressDialog(activity);
        dialog.setMessage(message);
        dialog.setOnCancelListener(listener);
        safelyShow();
    }

    /**
     * 显示提示等待的dialog
     */
    public static void showDefaulteMessageProgressDialogAddCancel(Activity activity, DialogInterface.OnCancelListener listener) {
        showMessageProgressDialogAddCancel(activity, activity.getString(R.string.please_wait), listener);
    }

    /**
     * 移除显示信息的dialog
     */
    public static void dismissProgressDialog() {
        if (dialog == null)
            return;
        try {
            dialog.dismiss();
        } catch (Exception e) {
            // 无论如何不能因为这个抛异常，
            Reporter.unreachable(e);
        }
        dialog = null;
    }

    public static boolean isShowing() {
        return dialog != null;
    }

    public static void tip(Context context, String tip) {
        ToastUtil.showToast(context, tip);
    }

    public static TipDialog tipDialog(Context context, String tip) {
        TipDialog dialog = new TipDialog(context);
        dialog.setTip(tip);
        dialog.show();
        return dialog;
    }

    public static void verify(Context ctx, String title, String tip, CharSequence hint, String text, int inputLength, VerifyDialog.VerifyClickListener listener) {
        VerifyDialog dialog = new VerifyDialog(ctx);
        dialog.setVerifyClickListener(title, tip, hint, text, inputLength, listener);
        dialog.show();
    }

    /**
     * 显示群开启群主验证，填写入群原因弹窗
     */
    public static void showGroupVerifyInputDialog(Context context, String userId, String jid) {
        VerifyDialog verifyDialog = new VerifyDialog(context);
        verifyDialog.setVerifyClickListener(MyApplication.getContext().getString(R.string.title_join_group_verify)
                , MyApplication.getContext().getString(R.string.tip_reason_invite_friends)
                , ""
                , new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {

                    }

                    @Override
                    public void send(String str) {
                        EventBus.getDefault().post(new EventSendVerifyMsg(userId, jid, str));
                    }
                });
        verifyDialog.show();
    }

    /**
     * 显示群开启群主验证，填写入群原因弹窗
     */
    public static void showGroupVerifyInputDialog(Context context, String userId, String jid, List<String> groupManagerUserAccidList) {
        VerifyDialog verifyDialog = new VerifyDialog(context);
        verifyDialog.setVerifyClickListener(MyApplication.getContext().getString(R.string.title_join_group_verify)
                , MyApplication.getContext().getString(R.string.tip_reason_invite_friends)
                , ""
                , new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {

                    }

                    @Override
                    public void send(String str) {
                        EventBus.getDefault().post(new EventSendVerifyMsg(userId, jid, str,groupManagerUserAccidList));
                    }
                });
        verifyDialog.show();
    }

    @NonNull
    private static CustomizeProgressDialog createProgressDialog(Context context) {
        // 这对话框不能出现多个，以免没能全部关闭，
        // 如果是多个activity都弹这个对话框还是可能有问题，本就不该有全局对话框，
        // TODO: 可以考虑弱引用不同activity创建不同dialog, 相同activity复用dialog, 但是dismiss也要传入activity,
        dismissProgressDialog();
        return new CustomizeProgressDialog(context);
    }

    /**
     * 显示提示message的dialog
     */
    public static void showMessageProgressDialog(Context context, String message) {
        dialog = createProgressDialog(context);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        safelyShow();
    }

    /**
     * 显示提示等待的dialog
     */
    public static void showDefaulteMessageProgressDialog(Context context) {
        showMessageProgressDialog(context, context.getString(R.string.please_wait));
    }

    /*final int maxLen = 20;

    // 根据ASCII判断
    InputFilter filter = new InputFilter() {
        *//**
     *
     * @param source 当前输入的字符
     * @param start  输入字符的起始长度
     * @param end    输入字符的终止长度
     * @param dest   当前显示的字符
     * @param dstart
     * @param dend
     * @return
     *//*
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            int count = 0;
            int asc = 0;
            while (count <= maxLen && asc < dest.length()) {
                char c = dest.charAt(asc++);
                if (c < 128) {
                    count = count + 1;
                } else {
                    count = count + 2;
                }
            }

            if (count > maxLen) {
                return dest.subSequence(0, asc - 1);
            }

            int asi = 0;
            while (count <= maxLen && asi < source.length()) {
                char c = source.charAt(asi++);
                if (c < 128) {
                    count = count + 1;
                } else {
                    count = count + 2;
                }
            }

            if (count > maxLen) {
                asi--;
            }

            return source.subSequence(0, asi);
        }
    };*/
}
