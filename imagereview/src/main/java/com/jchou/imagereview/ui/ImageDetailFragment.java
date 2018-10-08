package com.jchou.imagereview.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.jchou.imagereview.R;
import com.jchou.imagereview.glide.ProgressTarget;

import java.io.File;


/**
 * 单张图片显示Fragment
 */
public class ImageDetailFragment extends Fragment {
    private String mImageUrl;

    private PhotoView mImageView;


    private boolean isNewCreate = false, isVisible = false;//是否第一次加载完成，是否可见。

    public static ImageDetailFragment newInstance(String imageUrl) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putString("url", imageUrl);
        f.setArguments(args);

        return f;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isVisibleToUser) {
            initData();
        } else {
            isNewCreate = false;
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnLoadListener) {
            onLoadListener = (OnLoadListener) getActivity();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onLoadListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mImageUrl = bundle != null ? bundle.getString("url", "") : "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.image_detail_fragment,
                container, false);
        mImageView = (PhotoView) v.findViewById(R.id.image);
        mImageView.setScaleType(PhotoView.ScaleType.FIT_CENTER);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        isNewCreate = true;//布局新创建
        initData();
    }


    private void initData() {
        if (!isVisible || !isNewCreate) {
            return;
        }
        if (onImageListener != null) {
            onImageListener.onInit();
        }
        Glide.with(getActivity())
                .load(mImageUrl)
                .downloadOnly(new ProgressTarget<String, File>(mImageUrl, null) {

                    @Override
                    public void onLoadStarted(Drawable placeholder) {
                        super.onLoadStarted(placeholder);
                        if (onLoadListener != null) {
                            onLoadListener.onLoadStart();
                        }
                    }

                    @Override
                    public void onProgress(long bytesRead, long expectedLength) {
                        int p = 0;
                        if (expectedLength >= 0) {
                            p = (int) (100 * bytesRead / expectedLength);
                        }
                    }

                    @Override
                    public void onResourceReady(File resource, GlideAnimation<? super File> animation) {
                        mImageView.setImageURI(Uri.fromFile(resource));
                        ActivityCompat.startPostponedEnterTransition(getActivity());
                        if (onLoadListener != null) {
                            onLoadListener.onLoadSuccess();
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        ActivityCompat.startPostponedEnterTransition(getActivity());
                        if (onLoadListener != null) {
                            onLoadListener.onLoadFailed();
                        }
                    }

                    @Override
                    public void getSize(SizeReadyCallback cb) {
                        cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                    }
                });

    }


    public interface OnImageListener {
        void onInit();
    }

    public void setOnImageListener(OnImageListener onImageListener) {
        this.onImageListener = onImageListener;
    }

    private OnImageListener onImageListener;


    public interface OnLoadListener {
        void onLoadStart();

        void onLoadSuccess();

        void onLoadFailed();
    }

    private OnLoadListener onLoadListener;

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}