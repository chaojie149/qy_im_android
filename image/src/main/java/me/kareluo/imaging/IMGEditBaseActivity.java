package me.kareluo.imaging;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.ViewSwitcher;

import androidx.core.widget.ImageViewCompat;

import me.kareluo.imaging.core.IMGMode;
import me.kareluo.imaging.core.IMGText;
import me.kareluo.imaging.view.IMGColorGroup;
import me.kareluo.imaging.view.IMGView;

/**
 * Created by felix on 2017/12/5 下午3:08.
 *
 * @deprecated 使用sk模块里的类,
 */

abstract class IMGEditBaseActivity extends Activity implements View.OnClickListener,
        IMGTextEditDialog.Callback, RadioGroup.OnCheckedChangeListener,
        DialogInterface.OnShowListener, DialogInterface.OnDismissListener {

    public static final int OP_HIDE = -1;
    public static final int OP_NORMAL = 0;
    public static final int OP_CLIP = 1;
    public static final int OP_SUB_DOODLE = 0;
    public static final int OP_SUB_MOSAIC = 1;
    protected IMGView mImgView;
    private RadioGroup mModeGroup;
    private IMGColorGroup mColorGroup;
    private ImageView mEraser;
    private SeekBar sbSize;
    private IMGTextEditDialog mTextDialog;
    private View mLayoutOpSub;
    private ViewSwitcher mOpSwitcher, mOpSubSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            setContentView(R.layout.image_edit_activity);
            initViews();
            mImgView.setImageBitmap(bitmap);
            onCreated();
        } else finish();
    }

    public void onCreated() {

    }

    private void initViews() {
        mImgView = findViewById(R.id.image_canvas);
        mModeGroup = findViewById(R.id.rg_modes);

        mOpSwitcher = findViewById(R.id.vs_op);
        mOpSubSwitcher = findViewById(R.id.vs_op_sub);

        mColorGroup = findViewById(R.id.cg_colors);
        mColorGroup.setOnCheckedChangeListener(this);
        mEraser = findViewById(R.id.btn_eraser);
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_selected},
                new int[]{android.R.attr.state_selected}
        };

        int[] colors = new int[]{
                0xffffffff,
                0xff666666
        };
        ImageViewCompat.setImageTintList(mEraser, new ColorStateList(states, colors));

        mLayoutOpSub = findViewById(R.id.layout_op_sub);

        sbSize = findViewById(R.id.sbSize);
        resizeSeekBar(1f);
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = 5f * progress / seekBar.getMax();
                mImgView.setPenScale(scale);
                resizeSeekBar(scale);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void resizeSeekBar(float scale) {
        LayerDrawable drawable = (LayerDrawable) sbSize.getThumb();
        GradientDrawable gd = (GradientDrawable) drawable.getDrawable(1);
        gd.setSize((int) (scale * 20), (int) (scale * 20));
        sbSize.setThumb(drawable);
    }

    public int dip2px(float dpValue) {
        Context context = this;
        final float scale = context.getResources().getDisplayMetrics().density;

        return (int) (dpValue * scale + 0.5f);

    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.rb_doodle) {
            onModeClick(IMGMode.DOODLE);
        } else if (vid == R.id.btn_text) {
            onTextModeClick();
        } else if (vid == R.id.rb_mosaic) {
            onModeClick(IMGMode.MOSAIC);
        } else if (vid == R.id.btn_clip) {
            onModeClick(IMGMode.CLIP);
        } else if (vid == R.id.btn_eraser) {
            onEraser();
        } else if (vid == R.id.btn_undo) {
            onUndoClick();
        } else if (vid == R.id.tv_done) {
            onDoneClick();
        } else if (vid == R.id.tv_cancel) {
            onCancelClick();
        } else if (vid == R.id.ib_clip_cancel) {
            onCancelClipClick();
        } else if (vid == R.id.ib_clip_done) {
            onDoneClipClick();
        } else if (vid == R.id.tv_clip_reset) {
            onResetClipClick();
        } else if (vid == R.id.ib_clip_rotate) {
            onRotateClipClick();
        }
    }

    public void updateModeUI() {
        IMGMode mode = mImgView.getMode();
        switch (mode) {
            case DOODLE:
                mModeGroup.check(R.id.rb_doodle);
                setOpSubDisplay(OP_SUB_DOODLE);
                break;
            case MOSAIC:
                mModeGroup.check(R.id.rb_mosaic);
                setOpSubDisplay(OP_SUB_MOSAIC);
                break;
            case NONE:
                mModeGroup.clearCheck();
                setOpSubDisplay(OP_HIDE);
                break;
        }
    }

    public void onTextModeClick() {
        if (mTextDialog == null) {
            mTextDialog = new IMGTextEditDialog(this, this);
            mTextDialog.setOnShowListener(this);
            mTextDialog.setOnDismissListener(this);
        }
        mTextDialog.show();
    }

    @Override
    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        onColorChanged(mColorGroup.getCheckColor());
        mEraser.setSelected(false);
    }

    public void onEraser() {
        mImgView.setPenColor(0);
        mColorGroup.setOnCheckedChangeListener(null);
        mColorGroup.clearCheck();
        mColorGroup.setOnCheckedChangeListener(this);
        mEraser.setSelected(true);
    }

    public void setOpDisplay(int op) {
        if (op >= 0) {
            mOpSwitcher.setDisplayedChild(op);
        }
    }

    public void setOpSubDisplay(int opSub) {
        if (opSub < 0) {
            mLayoutOpSub.setVisibility(View.GONE);
        } else {
            mOpSubSwitcher.setDisplayedChild(opSub);
            mLayoutOpSub.setVisibility(View.VISIBLE);
            if (opSub == OP_SUB_DOODLE) {
                mEraser.setVisibility(View.VISIBLE);
            } else {
                mEraser.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mOpSwitcher.setVisibility(View.GONE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mOpSwitcher.setVisibility(View.VISIBLE);
    }

    public abstract Bitmap getBitmap();

    public abstract void onModeClick(IMGMode mode);

    public abstract void onUndoClick();

    public abstract void onCancelClick();

    public abstract void onDoneClick();

    public abstract void onCancelClipClick();

    public abstract void onDoneClipClick();

    public abstract void onResetClipClick();

    public abstract void onRotateClipClick();

    public abstract void onColorChanged(int checkedColor);

    @Override
    public abstract void onText(IMGText text);
}
