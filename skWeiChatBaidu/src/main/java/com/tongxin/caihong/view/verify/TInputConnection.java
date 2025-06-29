package com.tongxin.caihong.view.verify;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

public class TInputConnection extends InputConnectionWrapper {
    private BackspaceListener mBackspaceListener;

    public TInputConnection(InputConnection target, boolean mutable) {
        super(target, mutable);
    }

    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return this.mBackspaceListener != null && this.mBackspaceListener.onBackspace() ? true : super.deleteSurroundingText(beforeLength, afterLength);
    }

    public void setBackspaceListener(TInputConnection.BackspaceListener backspaceListener) {
        this.mBackspaceListener = backspaceListener;
    }

    public boolean sendKeyEvent(KeyEvent event) {
        return event.getKeyCode() == 67 && event.getAction() == 0 && this.mBackspaceListener != null && this.mBackspaceListener.onBackspace() ? true : super.sendKeyEvent(event);
    }

    public interface BackspaceListener {
        boolean onBackspace();
    }
}
