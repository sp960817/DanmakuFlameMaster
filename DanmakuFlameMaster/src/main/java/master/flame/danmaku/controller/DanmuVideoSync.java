package master.flame.danmaku.controller;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

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

        Log.i(TAG,"danmu video speed map = " + speedMaps);
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
        this.videoSpeed = videoSpeed;
        // 未开启动态计算
        if (this.dynamicallyAdjustSpeed) {
            drawHandler.setDanmuSpeed(videoSpeed);
            return;
        }
        float danmuSpeed = getSpeed(videoSpeed);
        this.repairSpeed = danmuSpeed;
        Log.i(TAG,"danmu video speed = " + videoSpeed + ", danmuSpeed=" + danmuSpeed);
        // 重置计算逻辑
        reset();
        drawHandler.setDanmuSpeed(danmuSpeed);
    }

    /**
     * 是否开启动态计算
     * @param dynamicallyAdjustSpeed 开启/关闭
     */
    public void setDynamicallyAdjustSpeed(boolean dynamicallyAdjustSpeed) {
        this.dynamicallyAdjustSpeed = dynamicallyAdjustSpeed;
        removeMessages(CALC_SPEED);
        if (dynamicallyAdjustSpeed) {
            reset();
        }
    }

    /**
     * 重置计算开始时间
     */
    public void reset() {
        this.dBaseTime = SystemClock.uptimeMillis();
        this.dVideoTime = DanmakuTimer.videoTime;
        this.preVideoTimeGap = 0;

        removeMessages(CALC_SPEED);
        sendEmptyMessageDelayed(CALC_SPEED, 60_1000);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        int what = msg.what;

        switch (what) {
            case CALC_SPEED:
                calc();
                break;
            case SET_SPEED:
                drawHandler.setVideoSpeed((Float) msg.obj);
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
        long currentTime = SystemClock.uptimeMillis();

        long timeGap = currentTime - dBaseTime;
        long videoTimeGap = currentVideoTime - dVideoTime;

        // 相差10秒内忽略
        if (Math.abs(timeGap - videoTimeGap) < 10_1000L) {
            return;
        }

        this.preVideoTimeGap = videoTimeGap;
        float tempRepairSpeed = videoTimeGap * 1.0f / timeGap;

        float newRepairSpeed = ((float) Math.round(tempRepairSpeed * 1000)) / 1000;
        if (DanmakuTimer.debug) {
            Log.d(TAG, "repair danmu speed , videoSpeed= " + videoSpeed + ", originDanmuSpeed=" + repairSpeed + ", newRepairSpeed=" + newRepairSpeed + ", videoTimeGap=" + videoTimeGap + ", danmuTimeGap=" + timeGap);
        }

        this.repairSpeed = newRepairSpeed;
        this.saveSpeed(videoSpeed, repairSpeed);

        sendMessage(obtainMessage(SET_SPEED, repairSpeed));
        sendEmptyMessage(DANMU_SEEK);
        long nextDelayTime = this.nextCalcTime();
        sendEmptyMessageDelayed(CALC_SPEED, nextDelayTime);
    }

    /**
     * 获取下次重新计算的时间
     * 延迟时间 1 1 2 4 8
     * 计算的时间间隔 1 2 4 8
     * @return 下次计算的时间
     */
    private long nextCalcTime() {
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
