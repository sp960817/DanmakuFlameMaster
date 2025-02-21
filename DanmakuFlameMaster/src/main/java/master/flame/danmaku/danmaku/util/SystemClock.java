package master.flame.danmaku.danmaku.util;

import android.util.Log;

import master.flame.danmaku.danmaku.model.DanmakuTimer;

/**
 * Created by ch on 15-12-9.
 */
public class SystemClock {
    /**
     * 使用系统时间类计算流逝时间
     */
    public static boolean useSystemClock = false;

    /**
     * 视频播放速度
     */
    private static float videoSpeed = 1.0f;

    /**
     * 基础时间
     */
    private static long baseTime = baseUptimeMillis();

    /**
     * 上次最后时间
     */
    private static long lastSystemClockTimeMillis = baseTime;
    private static boolean playing = true;


    public static long uptimeMillis() {
        if (useSystemClock) {
            return android.os.SystemClock.elapsedRealtime();
        }
        return calcVideoBaseTime();
    }

    private static long baseUptimeMillis() {
        return System.currentTimeMillis();
    }

    public static void sleep(long mills) {
        android.os.SystemClock.sleep(mills);
    }

    /**
     * 根据视频时间计算流逝时间
     * @return 流逝时间
     */
    private static long calcVideoBaseTime() {
        long gap = baseUptimeMillis() - lastSystemClockTimeMillis;
        long a = gap;
        if (SystemClock.playing && videoSpeed != 1.0f) {
            a = (long) ((gap) * videoSpeed);
        }

        long real = baseTime + a;
        if (DanmakuTimer.debug) {
            Log.d("SystemClock", "基础时间=" + baseTime + ", gap=" + gap + " * " + videoSpeed + " 计算后gap=" + a + ", 实际=" + real + ", 弹幕时间 " + DanmakuTimer.formatTime(real) + ", 视频时间 " + DanmakuTimer.formatTime(DanmakuTimer.videoTime));
        }
        return real;
    }

    /**
     * 标记当前播放器状态
     * @param playing 是否播放中
     */
    public static void setPlaying(boolean playing) {
        reset();
        SystemClock.playing = playing;
    }

    /**
     * 重新记录当前时间为基础时间
     */
    public static void reset() {
        baseTime = baseUptimeMillis();
        lastSystemClockTimeMillis = baseTime;
    }

    /**
     * 修改视频速度
     * @param videoSpeed 视频速度
     */
    public static void setVideoSpeed(float videoSpeed) {
        reset();
        SystemClock.videoSpeed = videoSpeed;
    }
}
