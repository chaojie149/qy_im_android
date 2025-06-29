package com.tongxin.caihong.view.verify;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.appcompat.widget.AppCompatEditText;

public class PwdEditText extends AppCompatEditText {
    private TInputConnection inputConnection;

    public PwdEditText(Context context) {
        super(context);
        this.init();
    }

    public PwdEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public PwdEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        this.inputConnection = new TInputConnection((InputConnection) null, true);
    }

    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        this.inputConnection.setTarget(super.onCreateInputConnection(outAttrs));
        return this.inputConnection;
    }

    public void setBackSpaceListener(TInputConnection.BackspaceListener backSpaceLisetener) {
        this.inputConnection.setBackspaceListener(backSpaceLisetener);
    }
}
