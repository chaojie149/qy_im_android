package com.tongxin.caihong.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.WorkerThread;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.GifRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.StringSignature;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.UserAvatarDao;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class ImageLoadHelper {
    private static final String TAG = "ImageLoadHelper";

    public static void loadBitmapDontAnimateWithPlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            BitmapSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadBitmap(ctx, url, placeholder, error, false,
                true, false, false, onSuccess, onError);
    }

    public static void loadBitmapDontAnimate(
            Context ctx,
            String url,
            BitmapSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadBitmap(ctx, url, null, null, false,
                true, false, false, onSuccess, onError);
    }

    public static void loadBitmapCenterCropDontAnimate(
            Context ctx,
            String url,
            BitmapSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadBitmap(ctx, url, null, null, true,
                true, false, false, onSuccess, onError);
    }

    public static void loadBitmapCenterCropDontAnimateWithError(
            Context ctx,
            String url,
            @DrawableRes int error,
            BitmapSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadBitmap(ctx, url, null, error, true,
                true, false, false, onSuccess, onError);
    }

    private static void loadBitmap(
            Context ctx,
            String url,
            @DrawableRes Integer placeholder,
            @DrawableRes Integer error,
            boolean centerCrop,
            boolean dontAnimate,
            boolean skipMemoryCache,
            boolean skipDiskCache,
            BitmapSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        BitmapRequestBuilder<String, Bitmap> request = Glide.with(ctx)
                .load(url)
                .asBitmap()
                .listener(new RequestListener<Object, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }
                });
        if (placeholder != null) {
            request.placeholder(placeholder);
        }
        if (error != null) {
            request.error(error);
        }
        if (centerCrop) {
            request.centerCrop();
        }
        if (dontAnimate) {
            request.dontAnimate();
        }
        if (skipMemoryCache) {
            request.skipMemoryCache(true);
        }
        if (skipDiskCache) {
            request.diskCacheStrategy(DiskCacheStrategy.NONE);
        }
        request.into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                onSuccess.onSuccess(resource);
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                onError.onFailed(e);
            }
        });
    }

    public static void loadImageDontAnimateWithPlaceholder(
            Context ctx,
            String url,
            @DrawableRes Integer placeholder,
            DrawableSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadImage(ctx, url, placeholder, null, null, true,
                onSuccess, onError);
    }

    public static void loadImageSignatureDontAnimateWithDrawablePlaceHolder(
            Context ctx,
            String url,
            Drawable placeholder,
            String signature, ImageView view
    ) {
        loadImageDrawable(ctx, url, placeholder, placeholder, signature, true,
                view);
    }

    private static void loadImageDrawable(
            Context ctx,
            String url,
            Drawable placeholder,
            Drawable error,
            String signature,
            boolean dontAnimate,
            ImageView view
    ) {
        view.setTag(R.id.key_avatar, url);
        DrawableRequestBuilder<String> request = Glide.with(ctx)
                .load(url)
                .listener(new RequestListener<Object, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<GlideDrawable> target, boolean isFirstResource) {
                        Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }
                });
        if (placeholder != null) {
            request.crossFade();
            request.placeholder(placeholder);
        }
        if (error != null) {
            request.error(error);
        }
        if (signature != null) {
            request.signature(new StringSignature(signature));
        }
        if (dontAnimate) {
            request.dontAnimate();
        }

        if (view.getTag(R.id.key_avatar) != url) {
            return;
        }
        request.into(view);

    }

    public static void loadImageSignatureDontAnimateWithPlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            String signature,
            DrawableSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadImage(ctx, url, placeholder, null, signature, true,
                onSuccess, onError);
    }

    private static void loadImage(
            Context ctx,
            String url,
            @DrawableRes Integer placeholder,
            @DrawableRes Integer error,
            String signature,
            boolean dontAnimate,
            DrawableSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        DrawableRequestBuilder<String> request = Glide.with(ctx)
                .load(url)
                .listener(new RequestListener<Object, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<GlideDrawable> target, boolean isFirstResource) {
                        Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }
                });
        if (placeholder != null) {
            request.placeholder(placeholder);
        }
        if (error != null) {
            request.error(error);
        }
        if (signature != null) {
            request.signature(new StringSignature(signature));
        }
        if (dontAnimate) {
            request.dontAnimate();
        }
        request.into(new SimpleTarget<GlideDrawable>() {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                onSuccess.onSuccess(resource);
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                onError.onFailed(e);
            }
        });
    }

    @WorkerThread
    public static Bitmap getBitmapCenterCrop(
            Context ctx,
            String url,
            String userId,
            int width,
            int height
    ) throws ExecutionException, InterruptedException {
        BitmapRequestBuilder<String, Bitmap> request = Glide.with(ctx)
                .load(url)
                .asBitmap()
                .signature((new StringSignature(UserAvatarDao.getInstance().getUpdateTime(userId))))
                .listener(new RequestListener<Object, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }
                });
        request.centerCrop();
        return request.into(width, height).get();
    }

    public static void showImageDontAnimateWithPlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, url, null, placeholder, error, null, null, null, false,
                false, true, view);
    }

    public static void showImageDontAnimateWithError(
            Context ctx,
            String url,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, url, null, null, error, null, null, null, false,
                false, true, view);
    }

    public static void showFileCenterCropWithSizePlaceHolder(
            Context ctx,
            File file,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            int width,
            int height,
            ImageView view
    ) {
        showImage(ctx, file, null, placeholder, error, width, height, null, true,
                false, false, view);
    }

    public static void showFileCenterCropWithSizeError(
            Context ctx,
            File file,
            @DrawableRes int error,
            int width,
            int height,
            ImageView view
    ) {
        showImage(ctx, file, null, null, error, width, height, null, true,
                false, false, view);
    }

    public static void showImageWithPlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, url, null, placeholder, error, null, null, null, false,
                false, false, view);
    }

    public static void showImageSignature(
            Context ctx,
            String url,
            @DrawableRes int error,
            String signature,
            ImageView view
    ) {
        showImage(ctx, url, null, null, error, null, null, signature, false,
                false, false, view);
    }

    public static void showFileWithError(
            Context ctx,
            File file,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, file, null, null, error, null, null, null,
                false, false, false, view);
    }

    public static void showImageWithError(
            Context ctx,
            String url,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, url, null, null, error, null, null, null, false,
                false, false, view);
    }

    public static void showImageCenterCropWithSize(
            Context ctx,
            String url,
            int width,
            int height,
            ImageView view
    ) {
        showImage(ctx, url, null, null, null, width, height, null, true,
                false, false, view);
    }

    public static void showImageWithSize(
            Context ctx,
            String url,
            int width,
            int height,
            ImageView view
    ) {
        showImage(ctx, url, null, null, null, width, height, null, false,
                false, false, view);
    }

    public static void showImageWithSizeError(
            Context ctx,
            String url,
            @DrawableRes int error,
            int width,
            int height,
            ImageView view
    ) {
        showImage(ctx, url, null, null, error, width, height, null, false,
                false, false, view);
    }

    public static void showImageWithSizePlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            int width,
            int height,
            ImageView view
    ) {
        showImage(ctx, url, null, placeholder, error, width, height, null, false,
                false, false, view);
    }

    public static void showImageCenterCrop(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, url, null, placeholder, error, null, null, null,
                true, false, false, view);
    }

    private static void showImage(
            Context ctx,
            Object model,
            DrawableRequestBuilder thumbnail,
            @DrawableRes Integer placeholder,
            @DrawableRes Integer error,
            Integer width,
            Integer height,
            String signature,
            boolean centerCrop,
            boolean crossFade,
            boolean dontAnimate,
            ImageView view
    ) {
        DrawableRequestBuilder<Object> request = Glide.with(ctx)
                .load(model)
                .listener(new RequestListener<Object, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<GlideDrawable> target, boolean isFirstResource) {
                        Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }
                });
        if (thumbnail != null) {
            request.thumbnail(thumbnail);
        }
        if (placeholder != null) {
            request.placeholder(placeholder);
        }
        if (error != null) {
            request.error(error);
        }
        if (width != null && height != null) {
            request.override(width, height);
        }
        if (signature != null) {
            request.signature(new StringSignature(signature));
        }
        if (centerCrop) {
            request.centerCrop();
        }
        if (crossFade) {
            request.crossFade();
        }
        if (dontAnimate) {
            request.dontAnimate();
        }
        request.into(view);
    }

    public static void showFile(
            Context ctx,
            File file,
            ImageView view
    ) {
        showImage(ctx, file, null, null, null, null, null, null,
                false, false, false, view);
    }

    public static void showGif(
            Context ctx,
            String url,
            ImageView view
    ) {
        showGif(ctx, url, null, null, null, null, false, view);
    }

    private static void showGif(
            Context ctx,
            String url,
            @DrawableRes Integer placeholder,
            @DrawableRes Integer error,
            Integer width,
            Integer height,
            boolean centerCrop,
            ImageView view
    ) {
        GifRequestBuilder<String> request = Glide.with(ctx)
                .load(url)
                .asGif()
                .listener(new RequestListener<Object, GifDrawable>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                        return false;
                    }
                });
        if (placeholder != null) {
            request.placeholder(placeholder);
        }
        if (error != null) {
            request.error(error);
        }
        if (width != null && height != null) {
            request.override(width, height);
        }
        if (centerCrop) {
            request.centerCrop();
        }
        request.diskCacheStrategy(DiskCacheStrategy.SOURCE);
        request.into(view);
    }

    public static void showGifWithPlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            ImageView view
    ) {
        showGif(ctx, url, placeholder, error, null, null, false, view);
    }

    public static void showGifCenterCropWithSizePlaceHolder(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            int width,
            int height,
            ImageView view
    ) {
        showGif(ctx, url, placeholder, error, width, height, true, view);
    }

    public static void showGifWithError(
            Context ctx,
            String url,
            @DrawableRes int error,
            ImageView view
    ) {
        showGif(ctx, url, null, error, null, null, false, view);
    }

    public static void loadFile(
            Context ctx,
            String url,
            FileSuccessCallback onSuccess
    ) {
        loadFile(ctx, url, onSuccess, e -> {
        });
    }

    public static void loadFile(
            Context ctx,
            String url,
            FileSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        DrawableTypeRequest<String> request = Glide.with(ctx)
                .load(url);
        request.listener(new RequestListener<Object, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, Object model, Target<GlideDrawable> target, boolean isFirstResource) {
                Log.d(TAG, "onException() called with: e = [" + e + "], model = [" + model + "], target = [" + target + "], isFirstResource = [" + isFirstResource + "]");
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                Log.d(TAG, "onResourceReady() called with: resource = [" + resource + "], model = [" + model + "], target = [" + target + "], isFromMemoryCache = [" + isFromMemoryCache + "], isFirstResource = [" + isFirstResource + "]");
                return false;
            }
        });
        request.downloadOnly(new SimpleTarget<File>() {
            @Override
            public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                onSuccess.onSuccess(resource);
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                onError.onFailed(e);
            }
        });
    }

    public static void showUriCrossFade(
            Context ctx,
            Uri uri,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, uri, null, null, error, null, null,
                null, false, true, false, view);
    }

    public static void showImage(
            Context ctx,
            String url,
            ImageView view
    ) {
        // TODO: 优先使用自己维护的缓存，
        showImage(ctx, url, null, null, null, null, null,
                null, false, false, false, view);
    }

    public static void showImage(
            Context ctx,
            String url,
            ImageView view,
            boolean t
    ) {
        DrawableRequestBuilder<String> thumbnail = null;
        if (url.contains("/o/")) {
            String s = url.replaceFirst("/o/", "/t/");
            // 先加载缩略图再加载原图，
            thumbnail = Glide.with(ctx)
                    .load(s)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
        }
        showImage(ctx, url, thumbnail, null, null, null, null,
                null, false, false, false, view);
    }

    public static void showImageWithoutAnimate(
            Context ctx,
            String url,
            @DrawableRes int placeholder,
            @DrawableRes int error,
            ImageView view
    ) {
        showImage(ctx, url, null, placeholder, error, null, null, null, false,
                false, true, view);
    }

    public static void loadBitmapWithoutCache(
            Context ctx,
            String url,
            BitmapSuccessCallback onSuccess,
            ImageFailedCallback onError
    ) {
        loadBitmap(ctx, url, null, null, false,
                false, true, true, onSuccess, onError);
    }

    public interface DrawableSuccessCallback {
        void onSuccess(Drawable d);
    }

    public interface BitmapSuccessCallback {
        void onSuccess(Bitmap b);
    }

    public interface ImageFailedCallback {
        void onFailed(Exception e);
    }

    public interface FileSuccessCallback {
        void onSuccess(File f);
    }
}
