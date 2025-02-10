/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.danmaku.model;

public class DanmakuTimer {
    public static boolean useOrigin = true;
    private static long videoTime = -1;

    public long currMillisecond = 0L;
    private long lastInterval;

    private float videoSpeed = 1.0f;
    private long lastTimeStamp = 0L;
    private long lastCurr;
    private long firstCurr;

    public DanmakuTimer() {
    }

    public DanmakuTimer(long curr) {
        update(curr);
    }

    /**
     * 设置外部真正时间
     */
    public static void setVideoTime(long videoTime) {
        DanmakuTimer.videoTime = videoTime;
    }

    public long getCurrMillisecond() {
//        if (true && DanmakuTimer.videoTime > 0) {
//            return videoTime;
//        }

        if (useOrigin || DanmakuTimer.videoTime < 0) {
            return currMillisecond;
        }

        return videoTime;
    }

    public long update(long curr) {
        if (useOrigin) {
            return originUpdate(curr);
        }

        if(lastTimeStamp == 0) {
            lastTimeStamp = System.currentTimeMillis();
            firstCurr = curr;
        }
        long t = System.currentTimeMillis();
        lastInterval = t - lastTimeStamp;

        if((lastInterval - curr + lastCurr) > 2000 || (lastInterval - curr + lastCurr) < -2000)
            currMillisecond = curr - firstCurr;
        else
            currMillisecond += (long) (lastInterval * videoSpeed);

        lastCurr = curr;
        lastTimeStamp = t;
        return lastInterval;
    }

    private long originUpdate(long curr) {
        lastInterval = curr - currMillisecond;
        currMillisecond = curr;
        return lastInterval;
    }

    public long add(long mills) {
        return update(currMillisecond + mills);
    }

    public long lastInterval() {
        return lastInterval;
    }

    public void setSpeed(float speed) {
        videoSpeed = speed;
    }

    public float getSpeed() {
        return videoSpeed;
    }
}
