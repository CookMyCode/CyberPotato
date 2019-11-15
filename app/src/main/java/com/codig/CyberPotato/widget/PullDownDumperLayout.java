package com.codig.CyberPotato.widget;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import static android.os.SystemClock.sleep;

public class PullDownDumperLayout extends LinearLayout implements View.OnTouchListener {

    /**
     * 取布局中的第一个子元素为下拉隐藏头部
     */
    private View mHeadLayout;

    /**
     * 隐藏头部布局的高的负值
     */
    private int mHeadLayoutHeight;

    /**
     * 隐藏头部的布局参数
     */
    private MarginLayoutParams mHeadLayoutParams;

    /**
     * 判断是否为第一次初始化，第一次初始化需要把headView移出界面外
     */
    private boolean mOnLayoutIsInit=false;

    /**
     * 从配置获取的滚动判断阈值，为两点间的距离，超过此阈值判断为滚动
     */
//    private int mScaledTouchSlop;

    /**
     * 按下时的y轴坐标
     */
//    private float mDownY;

    /**
     * 移动时，前一个坐标
     */
    private float mMoveY;

    /**
     * 如果为false，会退出头部展开或隐藏动画
     */
    private boolean mChangeHeadLayoutTopMargin;

    /**
     * 头部布局的隐藏和展开速度，以及单次执行时间
     */
    private int mHeadLayoutHideSpeed;
    private int mHeadLayoutUnfoldSpeed;
    private long mSleepTime;

    /**
     * 初始化头部布局的偏移值，数值越大，头部可见部分越多，预设值为0，即初始时头部完全不可见
     */
    private int mTopMarginOffset;

    /**
     * 触发动画的分界线，头部布局上半部分和整体高度的比例
     */
    private double mUnfoldRatio;
    private double mHideRatio;

    /**
     * 触发动画的分界线，初始值由mRatio计算得到
     * 头部处于隐藏时等于mUnfoldBoundary
     * 头部处于展开时等于mHideBoundary
     * mBoundary在onTouch的ACTION_DOWN中变化
     */
    private int mBoundary;
    private int mUnfoldBoundary;
    private int mHideBoundary;

    /**
     * 阻尼值，越大越难拖动，呈线性趋势
     */
    private int mDumper;

    public PullDownDumperLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
//        mScaledTouchSlop= ViewConfiguration.get(context).getScaledTouchSlop();
        mHeadLayoutHideSpeed=-30;
        mHeadLayoutUnfoldSpeed=30;
        mSleepTime=10;
        mUnfoldRatio=0.6;
        mHideRatio=mUnfoldRatio;
        mDumper=2;
        mTopMarginOffset=-400;
    }

    /**
     * 布局开始设置每一个控件
     * 在activity的onCreate执行之后才会执行
     * 因此可以在onCreate中调用set方法设置参数
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //只初始化一次
        if(!mOnLayoutIsInit && changed) {
            //将第一个子元素作为头部移出界面外
            mHeadLayout = this.getChildAt(0);
            mHeadLayoutHeight=-mHeadLayout.getHeight();
            mUnfoldBoundary=(int)(mUnfoldRatio*mHeadLayoutHeight);//计算触发展开动画分界线
            mHideBoundary=(int)(mHideRatio*mHeadLayoutHeight);//计算触发展开动画分界线
            mBoundary=mUnfoldBoundary;//触发动画的分界线初始为mUnfoldBoundary
            mHeadLayoutHeight-=mTopMarginOffset;//头部隐藏布局可见的部分
            mHeadLayoutParams=(MarginLayoutParams) mHeadLayout.getLayoutParams();
            mHeadLayoutParams.topMargin=mHeadLayoutHeight;
            mHeadLayout.setLayoutParams(mHeadLayoutParams);
            //设置手势监听器，不能触碰的控件需要添加android:clickable="true"
            getChildAt(1).setOnTouchListener(this);
            mHeadLayout.setOnTouchListener(this);
            //标记已被初始化
            mOnLayoutIsInit=true;
        }
    }

    /**
     * 屏幕触摸操作监听器
     * @return 返回false表示在执行onTouch后会继续执行onTouchEvent
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //根据此时处于完全展开或完全隐藏决定mBoundary的值，如果两种情况都不满足则不做改变
                if(mHeadLayoutParams.topMargin==mHeadLayoutHeight)
                    mBoundary=mUnfoldBoundary;
                else if(mHeadLayoutParams.topMargin==0)
                    mBoundary=mHideBoundary;

//                mDownY=event.getRawY();//获取按下的屏幕y坐标
                mMoveY=event.getRawY();
                mChangeHeadLayoutTopMargin=false;//false会打断隐藏或展开头部布局的动画
                break;
            case MotionEvent.ACTION_MOVE:
                float currY=event.getRawY();
                int vector=(int)(currY-mMoveY);//向量，用于判断手势的上滑和下滑
                mMoveY=currY;
                //判断是否为滑动
                if(Math.abs(vector)==0){
                    return false;
                }
                //头部完全隐藏时不再向上滑动
                if (vector < 0 && mHeadLayoutParams.topMargin <= mHeadLayoutHeight) {
                    return false;
                }
                //头部完全展开时不再向下滑动
                else if (vector > 0 && mHeadLayoutParams.topMargin >= 0) {
                    return false;
                }

                //对增量进行修正
                int topMargin = mHeadLayoutParams.topMargin + (vector/mDumper);
                if(topMargin>0){
                    // 瞬间拉动的距离超过了头部高度，因为这一瞬间很短，这里采用直接赋值的方式
                    // 如需实现平滑过渡，要另开线程，并且监听到ACTION_DOWN时线程可被打断
                    topMargin = 0;
                }
                else if(topMargin<mHeadLayoutHeight){
                    // 瞬间拉动的距离超过了头部高度，因为这一瞬间很短，这里采用直接赋值的方式
                    // 如需实现平滑过渡，要另开线程，并且监听ACTION_DOWN时线程可被打断
                    topMargin = mHeadLayoutHeight;
                }

                //使参数生效
                mHeadLayoutParams.topMargin = topMargin ;
                mHeadLayout.setLayoutParams(mHeadLayoutParams);
                break;
            default:
                //出现其他触碰事件，如MotionEvent.ACTION_UP时，根据阈值mBoundary判断此时头部应该弹出还是隐藏
                mChangeHeadLayoutTopMargin=true;//允许执行动画
                if(mHeadLayoutParams.topMargin<=mBoundary){
                    //隐藏
                    new MoveHeaderTask().execute(true);
                }
                else{
                    //展开
                    new MoveHeaderTask().execute(false);
                }
                break;
        }
        return false;
    }

    /**
     * 新线程，隐藏或者展开头部布局，线程可被ACTION_DOWN打断
     */
    private class MoveHeaderTask extends AsyncTask<Boolean, Integer, Integer> {

        /**
         *
         * @param opt true为隐藏动画，false为展开动画
         * @return
         */
        @Override
        protected Integer doInBackground(Boolean... opt) {
            int topMargin=mHeadLayoutParams.topMargin;
            //true为隐藏，false为展开
            int speed=(opt[0])?mHeadLayoutHideSpeed:mHeadLayoutUnfoldSpeed;
            while(mChangeHeadLayoutTopMargin){
                topMargin += speed;
                if (topMargin <= mHeadLayoutHeight||topMargin>=0) {
                    topMargin=(opt[0])?mHeadLayoutHeight:0;
                    publishProgress(topMargin);
                    break;
                }
                publishProgress(topMargin);
                sleep(mSleepTime);
            }
            return null;
        }

        //调用publishProgress后会执行
        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            mHeadLayoutParams.topMargin=topMargin[0];
            mHeadLayout.setLayoutParams(mHeadLayoutParams);
        }

    }

    //调整参数
    public void setHeadLayoutHideSpeed(int speed){
        this.mHeadLayoutHideSpeed=speed;
    }
    public void setHeadLayoutUnfoldSpeed(int speed){
        this.mHeadLayoutUnfoldSpeed=speed;
    }
    public void setSleepTime(long time){
        this.mSleepTime=time;
    }
    public void setDumper(int dumper){
        this.mDumper=dumper;
    }
    public void setTopMarginOffset(int offset){
        this.mTopMarginOffset=-offset;
    }

    /**
     * 头部处于隐藏状态时，触发展开动画的分界线
     * @param ratio 头部布局上部分与下部分的分界线
     */
    public void setUnfoldRatio(double ratio){
        this.mUnfoldRatio=ratio;
    }

    /**
     * 头部处于展开状态时，触发隐藏动画的分界线
     * @param ratio 头部布局上部分与下部分的分界线
     */
    public void setHideRatio(double ratio){
        this.mHideRatio=ratio;
    }
}