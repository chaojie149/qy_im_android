package me.kareluo.imaging.core.sticker;

import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by felix on 2017/11/17 下午6:08.
 */

public class IMGStickerMoveHelper {

    private static final String TAG = "IMGStickerMoveHelper";
    private static final Matrix M = new Matrix();
    private View mView;
    private float mX, mY;

    public IMGStickerMoveHelper(View view) {
        mView = view;
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mX = event.getX();
                mY = event.getY();
                M.reset();
                M.setRotate(v.getRotation());
                return true;
            case MotionEvent.ACTION_MOVE:
                float[] dxy = {event.getX() - mX, event.getY() - mY};
                M.mapPoints(dxy);
                v.setTranslationX(mView.getTranslationX() + dxy[0]);
                v.setTranslationY(mView.getTranslationY() + dxy[1]);
                return true;
        }
        return false;
    }
}
