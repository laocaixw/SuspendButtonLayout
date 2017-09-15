package com.laocaixw.layout;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.laocaixw.suspendbuttonlayout.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SuspendButtonLayout extends RelativeLayout implements View.OnTouchListener {

    private float mScreenWidth, mScreenHeight; // 屏幕宽高，不包含状态栏

    private int mNumber; // 子按钮数量
    private int mDegree; // 按钮打开路径之间的角度
    private float mMarginY; // 上下方留出的空间
    private float mDistance; // 主按钮与子按钮间距
    private float mImageSize; // 按钮的大小

    private int[] mResImage = new int[6];
    private int mResMainClose, mResMainOpen;
    private List<ImageView> imageViewList = new ArrayList<>();
    private ImageView imageMain;

    private float lastX, lastY;
    private float downX, downY;
    private boolean suspendedInLeft = true;

    // 按钮状态
    public static final int SUSPEND_BUTTON_CLOSED = 0;
    public static final int SUSPEND_BUTTON_OPENED = 1;
    public static final int SUSPEND_BUTTON_CLOSING = 2;
    public static final int SUSPEND_BUTTON_OPENING = 3;
    public static final int SUSPEND_BUTTON_MOVING = 4;
    public static final int SUSPEND_BUTTON_MOVED = 5;
    private int suspendedStatus = 0;

    private boolean isInit = false;
    private boolean initPos = false;
    private int mStayPosY;

    private OnSuspendListener listener = null;

    public SuspendButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 获取屏幕宽高
        int statusBarHeight = 0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            statusBarHeight = getResources().getDimensionPixelSize(
                    Integer.parseInt(field.get(obj).toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        // mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels - statusBarHeight;

        // 获取属性
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.SuspendButtonLayout, 0, 0);
        try {
            // 子按钮数量，3-6之间，小于3默认为3，大于6默认为6
            mNumber = ta.getInteger(R.styleable.SuspendButtonLayout_number, 6);
            if (mNumber < 3) mNumber = 3;
            if (mNumber > 6) mNumber = 6;
            // 按钮打开路径之间的角度，根据子按钮数量而定
            mDegree = 180 / (mNumber - 1);

            float minScreenWH = Math.min(mScreenWidth, mScreenHeight);
            // 上下方留出的空间，默认分别小于宽高中较小者的一半
            mMarginY = ta.getDimension(R.styleable.SuspendButtonLayout_marginY, mScreenHeight / 3);
            if (mMarginY > (minScreenWH / 2)) mMarginY = minScreenWH / 2;

            // 主按钮与子按钮间距，默认小于等于上下方留出的空间
            mDistance = ta.getDimension(R.styleable.SuspendButtonLayout_distance, mMarginY);
            if (mDistance > mMarginY) mDistance = mMarginY;

            // 按钮的大小，默认小于主按钮与子按钮间距
            mImageSize = ta.getDimension(R.styleable.SuspendButtonLayout_imageSize,
                    mDistance / 2);
            if (mImageSize > mDistance) mImageSize = mDistance;

            // 按钮图片资源
            mResImage[0] = ta.getResourceId(R.styleable.SuspendButtonLayout_image1,
                    R.mipmap.suspend_1);
            mResImage[1] = ta.getResourceId(R.styleable.SuspendButtonLayout_image2,
                    R.mipmap.suspend_2);
            mResImage[2] = ta.getResourceId(R.styleable.SuspendButtonLayout_image3,
                    R.mipmap.suspend_3);
            mResImage[3] = ta.getResourceId(R.styleable.SuspendButtonLayout_image4,
                    R.mipmap.suspend_4);
            mResImage[4] = ta.getResourceId(R.styleable.SuspendButtonLayout_image5,
                    R.mipmap.suspend_5);
            mResImage[5] = ta.getResourceId(R.styleable.SuspendButtonLayout_image6,
                    R.mipmap.suspend_6);
            mResMainClose = ta.getResourceId(R.styleable.SuspendButtonLayout_imageMainClose,
                    R.mipmap.suspend_main_close);
            mResMainOpen = ta.getResourceId(R.styleable.SuspendButtonLayout_imageMainOpen,
                    R.mipmap.suspend_main_open);

            // 按钮布局
            LayoutParams params = new LayoutParams((int) mImageSize, (int) mImageSize);

            for (int i = 0; i < mNumber; i++) {
                ImageView iv = new ImageView(context);
                iv.setPadding(8, 8, 8, 8);
                iv.setImageResource(mResImage[i]);
                imageViewList.add(i, iv);
                final int index = i + 1;
                imageViewList.get(i).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onChildButtonClick(index);
                        }
                    }
                });
                addView(imageViewList.get(i), params);
            }
            imageMain = new ImageView(context);
            imageMain.setPadding(0, 0, 0, 0);
            imageMain.setImageResource(mResMainClose);
            addView(imageMain, params);
            imageMain.setOnTouchListener(this);

            // imageView_main在最后绘制，所以在imageView_main绘制完成后初始化移动几个控件
            ViewTreeObserver vto = imageMain.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    moveAnimSingle(imageMain, 0f, 0f, 0f, mMarginY, 0, false);
                    moveAnimInitAll(imageViewList, 0f, 0f, 0f, mMarginY, 0);
                    //noinspection deprecation
                    imageMain.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    suspendedStatus = SUSPEND_BUTTON_CLOSED;
                    // 隐藏子按钮
                    for (int i = 0; i < imageViewList.size(); i++) {
                        imageViewList.get(i).setVisibility(View.GONE);
                    }
                    isInit = true;
                    if (initPos) {
                        movePosition(!suspendedInLeft, mStayPosY);
                    }
                }
            });

        } finally {
            ta.recycle();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float viewX = v.getX();
        float viewY = v.getY();
        float eventX = event.getRawX();
        float eventY = event.getRawY();
        float startX, startY, endX, endY;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = eventX;
                lastY = eventY;
                downX = eventX;
                downY = eventY;
                break;
            case MotionEvent.ACTION_MOVE:
                startX = viewX;
                startY = viewY;
                endX = viewX + (eventX - lastX);
                endY = viewY + (eventY - lastY);
                if (suspendedStatus == SUSPEND_BUTTON_CLOSED) {
                    moveAnimSingle(imageMain, startX, startY, endX, endY, 0, false);
                    moveAnimAll(imageViewList, eventX - lastX, eventY - lastY, 0);
                    if (Math.abs(lastX - downX) > 1 && Math.abs(lastY - downY) > 1) {
                        suspendedStatus = SUSPEND_BUTTON_MOVING;
                        if (listener != null) {
                            listener.onButtonStatusChanged(suspendedStatus);
                        }
                        suspendedStatus = SUSPEND_BUTTON_CLOSED;
                    }
                }
                lastX = eventX;
                lastY = eventY;
                break;
            case MotionEvent.ACTION_UP:
                startX = viewX;
                startY = viewY;
                // 判断左右
                if ((viewX + (v.getWidth() / 2)) < (mScreenWidth / 2)) { // 左
                    suspendedInLeft = true;
                    endX = 0;
                } else { // 右
                    suspendedInLeft = false;
                    endX = mScreenWidth - v.getWidth();
                }
                // 判断上下
                if (viewY < mMarginY) {
                    endY = mMarginY;
                } else if ((viewY + v.getHeight()) > (mScreenHeight - mMarginY)) {
                    endY = mScreenHeight - mMarginY - v.getHeight();
                } else {
                    endY = viewY;
                }

                if (suspendedStatus == SUSPEND_BUTTON_CLOSED) {
                    moveAnimSingle(imageMain, startX, startY, endX, endY, 0, false);
                    moveAnimAll(imageViewList, endX - startX, endY - startY, 0);
                    if (Math.abs(lastX - downX) > 1 && Math.abs(lastY - downY) > 1) {
                        suspendedStatus = SUSPEND_BUTTON_MOVED;
                        if (listener != null) {
                            listener.onButtonStatusChanged(suspendedStatus);
                        }
                        suspendedStatus = SUSPEND_BUTTON_CLOSED;
                    }
                }

                // 点击事件，打开关闭
                if (Math.abs(lastX - downX) < 1 && Math.abs(lastY - downY) < 1) {
                    openSuspendButton();
                    closeSuspendButton();
                }

                break;
            default:
                break;
        }
        return true;
    }

    private void openAnim() {
        // 显示子按钮
        for (int i = 0; i < imageViewList.size(); i++) {
            imageViewList.get(i).setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < mNumber; i++) {
            float x, y;
            if (suspendedInLeft) {
                x = (float) (mDistance * Math.sin(Math.PI * (mDegree * i) / 180));
            } else {
                x = (float) (-mDistance * Math.sin(Math.PI * (mDegree * i) / 180));
            }
            y = (float) (-mDistance * Math.cos(Math.PI * (mDegree * i) / 180));
            float startX = imageViewList.get(i).getX();
            float startY = imageViewList.get(i).getY();
            float endX = startX + x;
            float endY = startY + y;
            moveAnimSingle(imageViewList.get(i), startX, startY, endX, endY, 500, true);
        }

        final float w1 = imageMain.getWidth();
        final float h1 = imageMain.getHeight();
        imageMain.setImageResource(mResMainOpen);
        imageMain.setPadding(8, 8, 8, 8);
        ViewTreeObserver vto = imageMain.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                float w2 = imageMain.getWidth();
                float h2 = imageMain.getHeight();
                float startX = imageMain.getX();
                float startY = imageMain.getY();
                float endX = startX + (w1 - w2) / 2;
                float endY = startY + (h1 - h2) / 2;
                moveAnimSingle(imageMain, startX, startY, endX, endY, 0, false);
                //noinspection deprecation
                imageMain.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    private void closeAnim() {
        for (int i = 0; i < mNumber; i++) {
            float x, y;
            if (suspendedInLeft) {
                x = (float) (-mDistance * Math.sin(Math.PI * (mDegree * i) / 180));
            } else {
                x = (float) (mDistance * Math.sin(Math.PI * (mDegree * i) / 180));
            }
            y = (float) (mDistance * Math.cos(Math.PI * (mDegree * i) / 180));
            float startX = imageViewList.get(i).getX();
            float startY = imageViewList.get(i).getY();
            float endX = startX + x;
            float endY = startY + y;
            moveAnimSingle(imageViewList.get(i), startX, startY, endX, endY, 500, true);
        }

        final float w1 = imageMain.getWidth();
        final float h1 = imageMain.getHeight();
        imageMain.setImageResource(mResMainClose);
        imageMain.setPadding(0, 0, 0, 0);
        ViewTreeObserver vto = imageMain.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                float w2 = imageMain.getWidth();
                float h2 = imageMain.getHeight();
                float startX = imageMain.getX();
                float startY = imageMain.getY();
                float endX = startX + (w1 - w2) / 2;
                float endY = startY + (h1 - h2) / 2;
                moveAnimSingle(imageMain, startX, startY, endX, endY, 0, false);
                //noinspection deprecation
                imageMain.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    private void moveAnimAll(List list, float moveX, float moveY, long duration) {
        for (int i = 0; i < list.size(); i++) {
            float startX = ((ImageView) list.get(i)).getX();
            float startY = ((ImageView) list.get(i)).getY();
            float endX = startX + moveX;
            float endY = startY + moveY;
            moveAnimSingle(list.get(i), startX, startY, endX, endY, duration, false);
        }
    }

    private void moveAnimInitAll(List list, float startX, float startY, float endX, float endY,
                                 long duration) {
        for (int i = 0; i < list.size(); i++) {
            float moveX = (imageMain.getWidth() - ((ImageView) list.get(i)).getWidth()) / 2;
            float moveY = (imageMain.getHeight() - ((ImageView) list.get(i)).getHeight()) / 2;
            moveAnimSingle(list.get(i), startX, startY, endX + moveX, endY + moveY, duration,
                    false);
        }
    }

    private void moveAnimSingle(Object object, float startX, float startY, float endX, float endY,
                                long duration, final boolean isAll) {
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(object, "translationX", startX, endX);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(object, "translationY", startY, endY);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animatorX, animatorY);
        set.setDuration(duration);
        set.start();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAll) {
                    switch (suspendedStatus) {
                        case SUSPEND_BUTTON_CLOSING:
                            suspendedStatus = SUSPEND_BUTTON_CLOSED;
                            if (listener != null) {
                                listener.onButtonStatusChanged(suspendedStatus);
                            }
                            break;
                        case SUSPEND_BUTTON_OPENING:
                            suspendedStatus = SUSPEND_BUTTON_OPENED;
                            if (listener != null) {
                                listener.onButtonStatusChanged(suspendedStatus);
                            }
                            break;
                        case SUSPEND_BUTTON_CLOSED:
                            // 隐藏子按钮
                            for (int i = 0; i < imageViewList.size(); i++) {
                                imageViewList.get(i).setVisibility(View.GONE);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * Hide SuspendButton
     */
    public void hideSuspendButton() {
        imageMain.setVisibility(View.GONE);
        for (int i = 0; i < imageViewList.size(); i++) {
            imageViewList.get(i).setVisibility(View.GONE);
        }
    }

    /**
     * Show SuspendButton
     */
    public void showSuspendButton() {
        imageMain.setVisibility(View.VISIBLE);
        if (suspendedStatus != SUSPEND_BUTTON_CLOSED) {
            for (int i = 0; i < imageViewList.size(); i++) {
                imageViewList.get(i).setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Open SuspendButton
     */
    public void openSuspendButton() {
        if (suspendedStatus == SUSPEND_BUTTON_CLOSED
                && imageMain.getVisibility() == View.VISIBLE) {
            suspendedStatus = SUSPEND_BUTTON_OPENING;
            if (listener != null) {
                listener.onButtonStatusChanged(suspendedStatus);
            }
            openAnim();
        }
    }

    /**
     * Close SuspendButton
     */
    public void closeSuspendButton() {
        if (suspendedStatus == SUSPEND_BUTTON_OPENED
                && imageMain.getVisibility() == View.VISIBLE) {
            suspendedStatus = SUSPEND_BUTTON_CLOSING;
            if (listener != null) {
                listener.onButtonStatusChanged(suspendedStatus);
            }
            closeAnim();
        }
    }

    /**
     * Set Child Button Image Resource
     *
     * @param index Child Button Position, 1-6
     * @param res   Resource
     */
    public void setChildImageResource(int index, int res) {
        if (index > 0 && index < 7) {
            mResImage[index - 1] = res;
            imageViewList.get(index - 1).setImageResource(mResImage[index - 1]);
        }
    }

    /**
     * Set Main Button(Close) Image Resource
     *
     * @param res Resource
     */
    public void setMainCloseImageResource(int res) {
        mResMainClose = res;
        imageMain.setImageResource(mResMainClose);
    }

    /**
     * Set Main Button(Open) Image Resource
     *
     * @param res Resource
     */
    public void setMainOpenImageResource(int res) {
        mResMainOpen = res;
        if (suspendedStatus == SUSPEND_BUTTON_OPENED
                || suspendedStatus == SUSPEND_BUTTON_OPENING) {
            imageMain.setImageResource(mResMainOpen);
        }
    }

    /**
     * Set Button Position
     *
     * @param isRight  left or right
     * @param stayPosY 1-100
     */
    public void setPosition(boolean isRight, int stayPosY) {
        suspendedInLeft = !isRight;
        if (isInit) {
            movePosition(isRight, stayPosY);
        } else {
            initPos = true;
            mStayPosY = stayPosY;
        }
    }

    private void movePosition(boolean isRight, int stayPosY) {
        if (suspendedStatus == SUSPEND_BUTTON_CLOSED) {
            float endX = 0f;
            if (isRight) {
                endX = mScreenWidth - mImageSize;
            }

            if (stayPosY > 100) {
                stayPosY = 100;
            }
            float endY = mMarginY +
                    (mScreenHeight - mMarginY * 2 - mImageSize) * ((float) stayPosY / 100);

            moveAnimSingle(imageMain, 0f, 0f, endX, endY, 0, false);
            moveAnimInitAll(imageViewList, 0f, 0f, endX, endY, 0);
        }
    }

    /**
     * Set Listener
     *
     * @param listener Callback
     */
    public void setOnSuspendListener(OnSuspendListener listener) {
        this.listener = listener;
    }

    public interface OnSuspendListener {
        /**
         * Button Status Listener
         *
         * @param status Button Status
         *               <br>Closed：SuspendButtonLayout.SUSPEND_BUTTON_CLOSED
         *               <br>Opend：SuspendButtonLayout.SUSPEND_BUTTON_OPENED
         *               <br>Closing：SuspendButtonLayout.SUSPEND_BUTTON_CLOSING
         *               <br>Opening：SuspendButtonLayout.SUSPEND_BUTTON_OPENING
         *               <br>Moving：SuspendButtonLayout.SUSPEND_BUTTON_MOVING
         *               <br>Moved：SuspendButtonLayout.SUSPEND_BUTTON_MOVED
         */
        void onButtonStatusChanged(int status);

        /**
         * Child Button Click Listener
         *
         * @param index Child Button Position, 1-6
         */
        void onChildButtonClick(int index);
    }
}
