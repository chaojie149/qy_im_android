package me.kareluo.imaging.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import me.kareluo.imaging.IMGTextEditDialog;
import me.kareluo.imaging.R;
import me.kareluo.imaging.core.IMGText;

/**
 * Created by felix on 2017/11/14 下午7:27.
 */
public class IMGStickerTextView extends IMGStickerView implements IMGTextEditDialog.Callback {

    private static final String TAG = "IMGStickerTextView";
    private static final int PADDING = 26;
    private static final float TEXT_SIZE_SP = 24f;
    private static float mBaseTextSize = -1f;
    private TextView mTextView;
    private IMGText mText;
    private IMGTextEditDialog mDialog;

    public IMGStickerTextView(Context context) {
        this(context, null, 0);
    }

    public IMGStickerTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMGStickerTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onInitialize(Context context) {
        if (mBaseTextSize <= 0) {
            mBaseTextSize = TEXT_SIZE_SP;
        }
        super.onInitialize(context);
    }

    @Override
    public View onCreateContentView(Context context) {
        mTextView = new TextView(context);
        mTextView.setTextSize(mBaseTextSize);
        mTextView.setPadding(PADDING, PADDING, PADDING, PADDING);
        mTextView.setTextColor(Color.WHITE);

        return mTextView;
    }

    public IMGText getText() {
        return mText;
    }

    public void setText(IMGText text) {
        mText = text;
        if (mText != null && mTextView != null) {
            mTextView.setText(mText.getText());
            mTextView.setTextColor(mText.getRealColor());
            if (mText.isTintBackground()) {
                mTextView.setBackgroundResource(R.drawable.image_text_bg);
                mTextView.setBackgroundTintList(mText.getRealBackgroundTint());
            } else {
                mTextView.setBackground(null);
            }
        }
    }

    @Override
    public void onContentTap() {
        IMGTextEditDialog dialog = getDialog();
        dialog.setText(mText);
        dialog.show();
    }

    private IMGTextEditDialog getDialog() {
        if (mDialog == null) {
            mDialog = new IMGTextEditDialog(getContext(), this);
        }
        return mDialog;
    }

    @Override
    public void onText(IMGText text) {
        setText(text);
    }
}