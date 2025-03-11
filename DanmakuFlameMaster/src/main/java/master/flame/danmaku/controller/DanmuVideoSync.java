package master.flame.danmaku.controller;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.util.SystemClock;

public class DanmuVideoSync extends Handler {
    private static final int CALC_SPEED = 1;
    private static final int SET_SPEED = 2;
    private static final int DANMU_SEEK = 3;

    private static final String TAG = "DanmuVideoSync";

    private final Map<Float, Float> speedMaps;

    private DrawHandler drawHandler;

    public DanmuVideoSync(DrawHandler drawHandler) {
        this(new HashMap<>(), drawHandler);
    }

    public DanmuVideoSync(Map<Float, Float> speedMaps, DrawHandler drawHandler) {
        if (speedMaps == null) {
            throw new RuntimeException("速度map对象不能为空");
        }

        Log.i(TAG,"danmu video speed map = " + speedMaps + ", 开启动态计算=" + dynamicallyAdjustSpeed);
        this.speedMaps = speedMaps;
        this.drawHandler = drawHandler;
    }

    public float getSpeed(float videoSpeed) {
        Float changeSpeed = speedMaps.get(videoSpeed);
        return Objects.nonNull(changeSpeed) ? changeSpeed : videoSpeed;
    }

    private boolean dynamicallyAdjustSpeed;

    /**
     * 弹幕倍数
     */
    private float repairSpeed;
    private float videoSpeed;

    private long dBaseTime;
    private long dVideoTime;

    /**
     * 记录之前视频播放多少时长
     */
    private long preVideoTimeGap;

    /**
     * 设置视频播放速度
     * @param videoSpeed 视频播放速度
     */
    public void setVideoSpeed(float videoSpeed) {
        if (this.videoSpeed == videoSpeed) {
            return;
        }

        this.videoSpeed = videoSpeed;
        float danmuSpeed = getSpeed(videoSpeed);
        this.repairSpeed = danmuSpeed;
        Log.i(TAG,"danmu video set speed = " + videoSpeed + ", danmuSpeed=" + danmuSpeed + ", 开启动态计算=" + dynamicallyAdjustSpeed);
        // 未开启动态计算
        if (!this.dynamicallyAdjustSpeed) {
            drawHandler.setDanmuSpeed(videoSpeed);
            return;
        }
        // 重置计算逻辑
        reset(CHANGE_SPEED);
        drawHandler.setDanmuSpeed(danmuSpeed);
    }

    /**
     * 是否开启动态计算
     * @param dynamicallyAdjustSpeed 开启/关闭
     */
    public void setDynamicallyAdjustSpeed(boolean dynamicallyAdjustSpeed) {
        if (dynamicallyAdjustSpeed == this.dynamicallyAdjustSpeed) {
            return;
        }

        this.dynamicallyAdjustSpeed = dynamicallyAdjustSpeed;
        if (dynamicallyAdjustSpeed) {
            this.repairSpeed = getSpeed(videoSpeed);
            reset(CHANGE_SWITCH);
        } else {
            removeMessages(CALC_SPEED);
        }
    }


    @IntDef({START, SEEK, RESUME, CHANGE_SWITCH, CHANGE_SPEED})
    public @interface DanmuResetType {}
    public static final int START = 0;
    public static final int SEEK = 1;
    public static final int RESUME = 2;
    public static final int CHANGE_SWITCH = 3;
    public static final int CHANGE_SPEED = 4;

    /**
     * 重置计算开始时间
     */
    public void reset(@DanmuResetType int type) {
        this.dBaseTime = drawHandler.getCurrentTime();
        this.dVideoTime = DanmakuTimer.videoTime;
        this.preVideoTimeGap = 0;
        Log.i(TAG, "danmu video reset = " + videoSpeed + ", danmuSpeed=" + repairSpeed + ", 开启动态计算=" + dynamicallyAdjustSpeed + ", dBaseTime=" + dBaseTime + ", dVideoTime=" + dVideoTime + ", type=" + type);

        removeMessages(CALC_SPEED);
        if (dynamicallyAdjustSpeed) {
            sendEmptyMessageDelayed(CALC_SPEED, 60_000);
        }
    }

    public void quit() {
        this.dBaseTime = SystemClock.uptimeMillis();
        this.dVideoTime = DanmakuTimer.videoTime;
        this.preVideoTimeGap = 0;

        removeMessages(CALC_SPEED);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        int what = msg.what;

        Log.i(TAG,"danmu video receive msg = " + what + ", obj=" + msg.obj);
        switch (what) {
            case CALC_SPEED:
                calc();
                break;
            case SET_SPEED:
                drawHandler.setDanmuSpeed((Float) msg.obj);
                break;
            case DANMU_SEEK:
                drawHandler.seekTo(DanmakuTimer.videoTime);
                break;
        }
    }

    /**
     * 计算
     */
    private void calc() {
        if (!dynamicallyAdjustSpeed || DanmakuTimer.videoTime == 0L) {
            return;
        }

        long currentVideoTime = DanmakuTimer.videoTime;
        long currentTime = drawHandler.getCurrentTime();

        long timeGap = currentTime - dBaseTime;
        long videoTimeGap = currentVideoTime - dVideoTime;

        if (DanmakuTimer.debug) {
            Log.d(TAG, "当前视频和弹幕时间 , videoSpeed= " + videoSpeed + ", originDanmuSpeed=" + repairSpeed + ", videoTimeGap=" + videoTimeGap + ", danmuTimeGap=" + timeGap + ", currentVideoTime=" + currentVideoTime + ", currentDanmuTime=" + currentTime);
        }
        // 相差10秒内忽略
        long absTimeGap = Math.abs(timeGap - videoTimeGap);
        if (absTimeGap < 10_000L) {
            long nextDelayTime = this.nextCalcTime();
            sendEmptyMessageDelayed(CALC_SPEED, nextDelayTime);
            Log.d(TAG, "本次时间间隔<10秒,忽略 , videoSpeed= " + videoSpeed + ", originDanmuSpeed=" + repairSpeed + ", videoTimeGap=" + videoTimeGap + ", danmuTimeGap=" + timeGap + ", currentVideoTime=" + currentVideoTime + ", currentDanmuTime=" + currentTime + ", 时间差=" + (absTimeGap / 1000L) + ", 下次触发时间=" + nextDelayTime);
            return;
        }

        // 计算合理的速度
        float tempRepairSpeed = calcNewDanmuSpeed(videoTimeGap, timeGap);

        // 保留3位小数
        float newRepairSpeed = ((float) Math.round(tempRepairSpeed * 1000)) / 1000;

        this.preVideoTimeGap = videoTimeGap;
        this.repairSpeed = newRepairSpeed;
        this.saveSpeed(videoSpeed, repairSpeed);

        sendMessage(obtainMessage(SET_SPEED, repairSpeed));
        // 触发seek
        sendEmptyMessage(DANMU_SEEK);
        long nextDelayTime = this.nextCalcTime();
        sendEmptyMessageDelayed(CALC_SPEED, nextDelayTime);

        if (DanmakuTimer.debug) {
            Log.d(TAG, "repair danmu speed , videoSpeed= " + videoSpeed + ", repairSpeed=" + repairSpeed + ", tempRepairSpeed=" + tempRepairSpeed + ", newRepairSpeed=" + newRepairSpeed + ", videoTimeGap=" + videoTimeGap + ", danmuTimeGap=" + timeGap + ", 时间差=" + (absTimeGap / 1000L) + ", 下次触发时间=" + nextDelayTime);
        }
    }

    private float calcNewDanmuSpeed(long videoTimeGap, long timeGap) {
        float tempRepairSpeed = videoTimeGap * videoSpeed / timeGap;
        if (tempRepairSpeed < repairSpeed) {
            tempRepairSpeed = tempRepairSpeed - 0.01f;
        }
        tempRepairSpeed += tempRepairSpeed < repairSpeed ? -0.01f : 0.01f;
        return tempRepairSpeed;
    }

    /**
     * 获取下次重新计算的时间
     * 延迟时间 1 1 2 4 8
     * 计算的时间间隔 1 2 4 8
     * @return 下次计算的时间
     */
    private long nextCalcTime() {
        if (preVideoTimeGap <= 60_000) {
            return 60_000;
        }

        return preVideoTimeGap;
    }

    /**
     * 保存视频速度和弹幕速度关系
     * @param videoSpeed 视频速度
     * @param danmuSpeed 弹幕速度
     */
    private void saveSpeed(float videoSpeed, float danmuSpeed) {
        this.speedMaps.put(videoSpeed, danmuSpeed);
    }
}
