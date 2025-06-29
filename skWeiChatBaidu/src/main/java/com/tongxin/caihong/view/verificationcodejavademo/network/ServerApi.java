package com.tongxin.caihong.view.verificationcodejavademo.network;


import com.tongxin.caihong.view.verificationcodejavademo.model.CaptchaCheckIt;
import com.tongxin.caihong.view.verificationcodejavademo.model.CaptchaGetIt;
import com.tongxin.caihong.view.verificationcodejavademo.model.WordCaptchaGetIt;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Date:2020/5/18
 * author:wuyan
 */
public interface ServerApi {

    //获取验证码
    @POST("captcha/get")
    Observable<BaseResponse<CaptchaGetIt>> getAsync(@Body RequestBody body);

    //获取文字的验证码
    @POST("captcha/get")
    Observable<BaseResponse<WordCaptchaGetIt>> getWordCaptchaAsync(@Body RequestBody body);

    //核对验证码
    @POST("captcha/check")
    Observable<BaseResponse<CaptchaCheckIt>> checkAsync(@Body RequestBody body);
}
