package com.jchou.imagereview.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import com.jchou.imagereview.adapter.ImagePagerAdapter;
import com.jchou.imagereview.util.ScreenUtils;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 * Created by Johnson on 2018/4/18.
 */

public class DragViewPager extends ViewPager implements View.OnClickListener {
    public static final int STATUS_NORMAL = 0;//正常浏览状态
    public static final int STATUS_MOVING = 1;//滑动状态
    public static final int STATUS_RESETTING = 2;//返回中状态
    public static final String TAG = "DragViewPager";


    public static final float MIN_SCALE_SIZE = 0.3f;//最小缩放比例
    public static final int BACK_DURATION = 300;//ms
    public static final int DRAG_GAP_PX = 50;

    private int currentStatus = STATUS_NORMAL;
    private int currentPageStatus;

    private float mDownX;
    private float mDownY;
    private float screenHeight;

    /**
     * 要缩放的View
     */
    private ImageView currentShowView;
    /**
     * 滑动速度检测类
     */
    private VelocityTracker mVelocityTracker;
    private IAnimClose iAnimClose;

    public void setIAnimClose(IAnimClose iAnimClose) {
        this.iAnimClose = iAnimClose;
    }

    public DragViewPager(Context context) {
        super(context);
        init(context);
    }

    public DragViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        screenHeight = ScreenUtils.getScreenHeight(context);
        setBackgroundColor(Color.BLACK);
        addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                currentPageStatus = state;
            }
        });
    }


    public void setCurrentShowView(ImageView currentShowView) {
        this.currentShowView = currentShowView;
        if (this.currentShowView != null) {
            this.currentShowView.setOnClickListener(this);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (getAdapter() instanceof ImagePagerAdapter) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "onInterceptTouchEvent: 是否拦截按下事件");
                    mDownX = ev.getRawX();
                    mDownY = ev.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "onInterceptTouchEvent: 是否拦截滑动事件");
                    int deltaX = Math.abs((int) (ev.getRawX() - mDownX));
                    int deltaY = Math.abs((int) (ev.getRawY() - mDownY));
                    Log.d(TAG, "onInterceptTouchEvent: 滑动的距离=" + deltaX + "////" + deltaY);
                    //在Y轴方向滑动的距离大于某个值而且大于X轴方向的距离就算有效的下拉动作
                    if (deltaY > DRAG_GAP_PX && deltaY > deltaX) {
                        Log.d(TAG, "onInterceptTouchEvent: 拦截事件");
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (currentStatus == STATUS_RESETTING)
            return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onTouchEvent: 滑动的按下事件");
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
                addIntoVelocity(ev); //加入滑动速度检测
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: 接收滑动事件");
                addIntoVelocity(ev);
                int deltaY = Math.abs((int) (ev.getRawY() - mDownY));
                //滑动距离少于阈值
                if (deltaY <= DRAG_GAP_PX && currentStatus != STATUS_MOVING)
                    return super.onTouchEvent(ev);
                //viewpager不在切换中，并且手指往下滑动，开始缩放
                if (currentPageStatus != SCROLL_STATE_DRAGGING && (deltaY > DRAG_GAP_PX || currentStatus == STATUS_MOVING)) {
                    moveView(ev.getRawX(), ev.getRawY());
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentStatus != STATUS_MOVING)
                    return super.onTouchEvent(ev);
                final float mUpX = ev.getRawX();
                final float mUpY = ev.getRawY();

                float vY = computeYVelocity();//松开时必须释放VelocityTracker资源
                if (vY >= 1200 || Math.abs(mUpY - mDownY) > screenHeight / 4) {
                    //下滑速度快，或者下滑距离超过屏幕高度的指定值，就关闭
                    if (iAnimClose != null) {
                        iAnimClose.onPictureRelease(currentShowView);
                    }
                } else {
                    resetReviewState(mUpX, mUpY);
                }
                break;
        }

        return super.onTouchEvent(ev);
    }

    //返回浏览状态
    private void resetReviewState(final float mUpX, final float mUpY) {
        currentStatus = STATUS_RESETTING;
        if (mUpY != mDownY) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(mUpY, mDownY);
            valueAnimator.setDuration(BACK_DURATION);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float mY = (float) animation.getAnimatedValue();
                    float percent = (mY - mDownY) / (mUpY - mDownY);
                    float mX = percent * (mUpX - mDownX) + mDownX;
                    moveView(mX, mY);
                    if (mY == mDownY) {
                        mDownY = 0;
                        mDownX = 0;
                        currentStatus = STATUS_NORMAL;
                    }
                }
            });
            valueAnimator.start();
        } else if (mUpX != mDownX) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(mUpX, mDownX);
            valueAnimator.setDuration(BACK_DURATION);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float mX = (float) animation.getAnimatedValue();
                    float percent = (mX - mDownX) / (mUpX - mDownX);
                    float mY = percent * (mUpY - mDownY) + mDownY;
                    moveView(mX, mY);
                    if (mX == mDownX) {
                        mDownY = 0;
                        mDownX = 0;
                        currentStatus = STATUS_NORMAL;
                    }
                }
            });
            valueAnimator.start();
        } else if (iAnimClose != null)
            iAnimClose.onPictureClick();
    }


    //移动View
    private void moveView(float movingX, float movingY) {
        if (currentShowView == null)
            return;
        currentStatus = STATUS_MOVING;
        //移动的距离
        float deltaX = movingX - mDownX;
        float deltaY = movingY - mDownY;
        float scale = 1f;
        float alphaPercent = 1f;
        // if (deltaY > 0) {
        scale = 1 - Math.abs(deltaY) / screenHeight;
        alphaPercent = 1 - Math.abs(deltaY) / (screenHeight / 2);
        //}
        //移动view
        ViewHelper.setTranslationX(currentShowView, deltaX);
        ViewHelper.setTranslationY(currentShowView, deltaY);
        //缩放和设置背景透明度
        scaleView(scale);
        setBackgroundColor(getBlackAlpha(alphaPercent));
    }

    //缩放View
    private void scaleView(float scale) {
        scale = Math.min(Math.max(scale, MIN_SCALE_SIZE), 1);
        ViewHelper.setScaleX(currentShowView, scale);
        ViewHelper.setScaleY(currentShowView, scale);
    }


    private int getBlackAlpha(float percent) {
        percent = Math.min(1, Math.max(0, percent));
        int intAlpha = (int) (percent * 255);
        return Color.argb(intAlpha, 0, 0, 0);
    }

    private void addIntoVelocity(MotionEvent event) {
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);
    }


    private float computeYVelocity() {
        float result = 0;
        if (mVelocityTracker != null) {
            mVelocityTracker.computeCurrentVelocity(1000); //初始化速率的单位，1000表示1秒
            result = mVelocityTracker.getYVelocity();
            releaseVelocity();
        }
        return result;
    }

    private void releaseVelocity() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (iAnimClose != null) {
            iAnimClose.onPictureClick();
        }
    }


    public interface IAnimClose {
        void onPictureClick();

        void onPictureRelease(View view);
    }


}
