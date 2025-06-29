package com.tongxin.caihong.bean;

import java.io.Serializable;
import java.util.List;

public class SignBean implements Serializable {

    /**
     * signPolicyAward : [10,0,10,0,10,0,10]
     * signCount : 0
     * isSign : 0
     */

    private int signCount;
    private int isSign;
    private List<Double> signPolicyAward;
    private int redPackDisconnect;

    public int getSignCount() {
        return signCount;
    }

    public void setSignCount(int signCount) {
        this.signCount = signCount;
    }

    public int getIsSign() {
        return isSign;
    }

    public void setIsSign(int isSign) {
        this.isSign = isSign;
    }

    public List<Double> getSignPolicyAward() {
        return signPolicyAward;
    }

    public void setSignPolicyAward(List<Double> signPolicyAward) {
        this.signPolicyAward = signPolicyAward;
    }

    public int getRedPackDisconnect() {
        return redPackDisconnect;
    }

    public void setRedPackDisconnect(int redPackDisconnect) {
        this.redPackDisconnect = redPackDisconnect;
    }
}
