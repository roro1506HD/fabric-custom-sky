package ovh.roro.customsky.util;

import net.minecraft.util.math.MathHelper;

public class SmoothFloat {

    private float valueLast;
    private float timeFadeUpSec;
    private float timeFadeDownSec;
    private long timeLastMs;

    public SmoothFloat(float valueLast, float timeFadeSec) {
        this(valueLast, timeFadeSec, timeFadeSec);
    }

    public SmoothFloat(float valueLast, float timeFadeUpSec, float timeFadeDownSec) {
        this.valueLast = valueLast;
        this.timeFadeUpSec = timeFadeUpSec;
        this.timeFadeDownSec = timeFadeDownSec;
        this.timeLastMs = System.currentTimeMillis();
    }

    public float getValueLast() {
        return this.valueLast;
    }

    public float getTimeFadeUpSec() {
        return this.timeFadeUpSec;
    }

    public float getTimeFadeDownSec() {
        return this.timeFadeDownSec;
    }

    public long getTimeLastMs() {
        return this.timeLastMs;
    }

    public float getSmoothValue(float value, float timeFadeUpSec, float timeFadeDownSec) {
        this.timeFadeUpSec = timeFadeUpSec;
        this.timeFadeDownSec = timeFadeDownSec;

        return this.getSmoothValue(value);
    }

    public float getSmoothValue(float value) {
        long currentTime = System.currentTimeMillis();
        float previousValue = this.valueLast;
        long previousTime = this.timeLastMs;
        float timeDeltaSec = (currentTime - previousTime) / 1000.0F;
        float timeFadeSec = value > previousValue ? this.timeFadeUpSec : this.timeFadeDownSec;
        float smoothValue = SmoothFloat.getSmoothValue(previousValue, value, timeDeltaSec, timeFadeSec);

        this.valueLast = smoothValue;
        this.timeLastMs = currentTime;

        return smoothValue;
    }

    public static float getSmoothValue(float previousValue, float value, float timeDeltaSec, float timeFadeSec) {
        if (timeDeltaSec <= 0.0F) {
            return previousValue;
        }

        float valueDelta = value - previousValue;
        float smoothValue;

        if (timeFadeSec > 0.0F && timeDeltaSec < timeFadeSec && Math.abs(valueDelta) > 0.000001f) {
            float countUpdates = timeFadeSec / timeDeltaSec;
            float k1 = 4.61F;
            float k2 = 0.13F;
            float k3 = 10.0F;
            float kCorr = k1 - 1.0F / (k2 + countUpdates / k3);
            float kTime = timeDeltaSec / timeFadeSec * kCorr;
            kTime = MathHelper.clamp(kTime, 0.0F, 1.0F);
            smoothValue = previousValue + valueDelta * kTime;
        } else {
            smoothValue = value;
        }

        return smoothValue;
    }
}
