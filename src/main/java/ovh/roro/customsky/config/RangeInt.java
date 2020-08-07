package ovh.roro.customsky.config;

public class RangeInt {

    private final int min;
    private final int max;

    public RangeInt(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public boolean isInRange(int value) {
        return value >= this.min && value <= this.max;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    @Override
    public String toString() {
        return "RangeInt{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
