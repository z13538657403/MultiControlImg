package com.test.zhangtao.activitytest;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by zhangtao on 17/1/2.
 */

import android.annotation.SuppressLint;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener, OnScaleGestureListener, OnTouchListener
{

    private boolean mOnce;

    /**
     * 初始化时放大的值
     */
    private float mInitScale;
    /**
     * 双击放大值到达的值
     */
    private float mMidScale;
    /**
     * 放大的最大
     */
    private float mMaxScale;

    private Matrix mScaleMatrix;

    /**
     * 铺货用户多指触碰时缩放比例
     */
    private ScaleGestureDetector mScaleGestureDetector;

    // --------------自由移动
    /**
     * 记录上一次多点触控的数量
     */
    private int mLastPointerCount;
    private float mLastX;
    private float mLastY;

    private int mTouchSlop;

    private boolean isCanDrag;

    //------------------双击放大与缩小
    private GestureDetector mGestureDetector;

    private boolean isAutoScale;

    public ZoomImageView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context)
    {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        mScaleMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mGestureDetector=new GestureDetector(context, new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                if (isAutoScale)
                {
                    return true;
                }
                float x=e.getX();
                float y=e.getY();

                if (getScale()<mMidScale)
                {
                    //缓慢放大
                    postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
                    isAutoScale=true;
                }
                else
                {
                    //缓慢缩小
                    postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
                    isAutoScale=true;
                }
                return true;
            }
        });
    }

    private class AutoScaleRunnable implements Runnable
    {
        /**
         * 缩放的目标值
         */
        private float mTargetScale;
        //缩放的中心点
        private float x;
        private float y;

        //缩放的梯度（每次缩放的大小）
        private final float BIGGER=1.08f;
        private final float SMALL=0.94f;

        private float tmpScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y)
        {
            super();
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if (getScale()<mTargetScale)
            {
                tmpScale=BIGGER;
            }
            if(getScale()>mTargetScale)
            {
                tmpScale=SMALL;
            }
        }

        @Override
        public void run()
        {
            //进行缩放
            mScaleMatrix.postScale(tmpScale, tmpScale,x,y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

            float currentScale=getScale();

            if ((tmpScale>1.0f&&currentScale<mTargetScale)
                    ||(tmpScale<1.0f&&currentScale>mTargetScale))
            {
                postDelayed(this, 16);//没16毫秒执行一次run方法
            }
            else
            {
                //设置我们的目标值
                float scale=mTargetScale/currentScale;
                mScaleMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);

                isAutoScale=false;
            }
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout()
    {
        if (!mOnce) {
            // 得到控件的宽和高
            int width = getWidth();
            int height = getHeight();
            // 得到我们的图片及宽和高
            Drawable d = getDrawable();
            if (d == null) {
                return;
            }
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();

            float scale = 1.0f;

            // 如果图片的宽度大于控件宽度，但是高度小于控件高度,将其缩小
            if (dw > width && dh < height) {
                scale = width * 1.0f / dw;
            }
            // 如果图片的高度大于控件gao度，但是宽度小于控件宽度,将其缩小
            if (dh > height && dw < width) {
                scale = height * 1.0f / dh;
            }
            // 如果图片的宽度大于控件宽度，但是高度da于控件高度
            if ((dw > width && dh > height) || (dw < width && dh < height)) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }

            // 得到了初始化是的缩放比例
            mInitScale = scale;
            mMidScale = mInitScale * 2;
            mMaxScale = mInitScale * 4;

            // 将图片移动到控件中心
            int dx = getWidth() / 2 - dw / 2;
            int dy = getHeight() / 2 - dh / 2;

            mScaleMatrix.postTranslate(dx, dy);// 平移
            mScaleMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);// 以控件的中心进行缩放
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }

    // 缩放区间：initScale-----maxScale
    // 实现多指触碰缩放操作
    @Override
    public boolean onScale(ScaleGestureDetector detector)
    {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();// 返回从前一个伸缩事件至当前伸缩事件的伸缩比率。

        if (getDrawable() == null)
        {
            return true;
        }
        // 缩放范围的控制,如果当前缩放值小于最大所放置，允许放大，如果当前缩放值大于最小缩放值，允许缩小
        if ((scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitScale && scaleFactor < 1.0f)) {
            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale;
            }
            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale;
            }
            // 以控件中心位置缩放
            // mScaleMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2,
            // getHeight() / 2);

            // 以多指触碰中心位置缩放 返回当前手势焦点的X坐标。
            // detector.getFocusX()如果手势正在进行中，焦点位于组成手势的两个触点之间。
            // 如果手势正在结束，焦点为仍留在屏幕上的触点的位置。
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector)
    {
        // 一定要返回true才会进入onScale()这个函数
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {}

    /**
     * 在缩放的时候进行边界控制
     */
    private void checkBorderAndCenterWhenScale()
    {
        RectF rect = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        // 缩放时进行边界检测，防止边沿出现空隙
        if (rect.width() >= width) {// 图片的宽大于等于控件的宽才需要移动
            if (rect.left > 0) {// 图片的左边沿距离控件的左边有距离
                deltaX = -rect.left;// 需左移的距离
            }
            if (rect.right < width) {// 图片的右边沿距离控件的右边有距离
                deltaX = width - rect.right;// 需右移的距离
            }
        }

        if (rect.height() >= height) {// 图片的高大于等于控件的高才需要移动
            if (rect.top > 0) {// 图片的上边沿距离控件的上边有距离
                deltaY = -rect.top;// 需上移的距离
            }
            if (rect.bottom < height) {// 图片的低边沿距离控件的低边有距离
                deltaY = height - rect.bottom;// 需上移的距离
            }
        }

        // 如果宽度或者高度小于控件的宽或高，则让其居中
        if (rect.width() < width)
        {
            deltaX = width / 2 - rect.right + rect.width() / 2;
        }
        if (rect.height() < height)
        {
            deltaY = height / 2 - rect.bottom + rect.height() / 2;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {

        if (mGestureDetector.onTouchEvent(event))
        {
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);
        //------------------处理放大后移动查看隐藏的部分--------------
        float x = 0;
        float y = 0;
        // 拿到多点触控的数量
        int pointerCount = event.getPointerCount();//返回MotionEvent中表示了多少手指数
        for (int i = 0; i < pointerCount; i++)
        {
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;

        if (mLastPointerCount != pointerCount)
        {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;

        RectF rectF = getMatrixRectF();
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if (rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01)
                {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:

                float dx = x - mLastX;
                float dy = y - mLastY;

                if (rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01)
                {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (!isCanDrag)
                {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag)
                {
                    if (getDrawable() != null)
                    {
                        // 如果宽度小于控件宽度，不允许横向移动
                        if (rectF.width() < getWidth())
                        {
                            dx = 0;
                        }
                        // 如果高度小于控件高度，不允许纵向移动
                        if (rectF.height() < getHeight())
                        {
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx, dy);
                        // 平移的时候检查边沿
                        checkBorderAndCenterWhenScale();
                        setImageMatrix(mScaleMatrix);
                    }
                }

                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;

            default:
                break;
        }
        return true;
    }

    /**
     * 获得图片放大缩小以后的宽和高以及t,b,l,r
     */
    private RectF getMatrixRectF()
    {
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();

        Drawable d = getDrawable();
        if (d != null)
        {
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    /**
     * 获取当前图片的缩放值
     */
    public float getScale()
    {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /**
     * 判断是否move
     */
    private boolean isMoveAction(float dx, float dy)
    {
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }
}
