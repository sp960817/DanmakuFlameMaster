
package master.flame.danmaku.controller;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDanmakuView {
    List<AfterDrawHandlerCall> afterHandlerCall = new ArrayList<>();
    interface AfterDrawHandlerCall {
        void handlerCall(DrawHandler drawHandler);
    }

    public final static int THREAD_TYPE_NORMAL_PRIORITY = 0x0;
    public final static int THREAD_TYPE_MAIN_THREAD = 0x1;
    public final static int THREAD_TYPE_HIGH_PRIORITY = 0x2;
    public final static int THREAD_TYPE_LOW_PRIORITY = 0x3;
    

    public boolean isPrepared();
    
    public boolean isPaused();

    public boolean isHardwareAccelerated();
    /**
     * 
     * @param type One of THREAD_TYPE_MAIN_THREAD, THREAD_TYPE_HIGH_PRIORITY, THREAD_TYPE_NORMAL_PRIORITY, or THREAD_TYPE_LOW_PRIORITY.
     */
    public void setDrawingThreadType(int type);

    public void enableDanmakuDrawingCache(boolean enable);

    public boolean isDanmakuDrawingCacheEnabled();

    public void showFPS(boolean show);

    default void setVideoSpeed(float videoSpeed) {
        setVideoSpeed(videoSpeed, false);
    }

    /**
     * 修改视频速度和是否开启速度自动调整
     * @param videoSpeed 视频速度
     * @param dynamicallyAdjustSpeed null-不修改原有设置, true-开启, false-关闭
     */
    void setVideoSpeed(float videoSpeed, Boolean dynamicallyAdjustSpeed);

    default void executeAfterHandlerInit(DrawHandler drawHandler) {
        // 全局唯一
        synchronized (IDanmakuView.class) {
            for (AfterDrawHandlerCall afterDrawHandlerCall : afterHandlerCall) {
                afterDrawHandlerCall.handlerCall(drawHandler);
            }
            afterHandlerCall.clear();
        }
    }

    default void addAfterHandlerInit(AfterDrawHandlerCall afterDrawHandlerCall) {
        afterHandlerCall.add(afterDrawHandlerCall);
    }

    void setDynamicallyAdjustSpeed(boolean dynamicallyAdjustSpeed);

    /**
     * 时间偏移秒数
     * @param offsetTime 偏移时间
     */
    void setOffsetTime(int offsetTime);
    
    /**
     * danmaku.isLive == true的情况下,请在非UI线程中使用此方法,避免可能卡住主线程
     * @param item
     */
    public void addDanmaku(BaseDanmaku item);

    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure);
    
    public void removeAllDanmakus(boolean isClearDanmakusOnScreen);
    
    public void removeAllLiveDanmakus();

    public IDanmakus getCurrentVisibleDanmakus();
    
    public void setCallback(Callback callback);
    
    /**
     * for getting the accurate play-time. use this method intead of parser.getTimer().currMillisecond
     * @return
     */
    public long getCurrentTime();

    public DanmakuContext getConfig();
    
    // ------------- Android View方法  --------------------
    
    public View getView();

    public int getWidth();

    public int getHeight();

    public void setVisibility(int visibility);
    
    public boolean isShown();
    

    // ------------- 播放控制 -------------------
    
    public void prepare(BaseDanmakuParser parser, DanmakuContext config);

    public void seekTo(Long ms);

    public void start();

    public void start(long postion);

    public void stop();

    public void pause();

    public void resume();

    public void release();
    
    public void toggle();
    
    public void show();
    
    public void hide();
    
    /**
     * show the danmakuview again if you called hideAndPauseDrawTask()
     * @param position The position you want to resume
     * @see #hideAndPauseDrawTask
     */
    public void showAndResumeDrawTask(Long position);
    
    /**
     * hide the danmakuview and pause the drawtask
     * @return the paused position
     * @see #showAndResumeDrawTask
     */
    public long hideAndPauseDrawTask();

    public void clearDanmakusOnScreen();

    // ------------- Click Listener -------------------
    public interface OnDanmakuClickListener {
        /**
         * @param danmakus all to be clicked, this value may be empty;
         *                 danmakus.last() is the latest danmaku which may be null;
         * @return True if the event was handled, false otherwise.
         */
        boolean onDanmakuClick(IDanmakus danmakus);

        boolean onDanmakuLongClick(IDanmakus danmakus);

        boolean onViewClick(IDanmakuView view);
    }

    public void setOnDanmakuClickListener(OnDanmakuClickListener listener);

    public void setOnDanmakuClickListener(OnDanmakuClickListener listener, float xOff, float yOff);

    public OnDanmakuClickListener getOnDanmakuClickListener();

    public float getXOff();

    public float getYOff();

    void forceRender();
}
