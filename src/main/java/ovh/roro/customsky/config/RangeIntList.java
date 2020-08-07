package ovh.roro.customsky.config;

import java.util.Arrays;
import ovh.roro.customsky.util.Util;

public class RangeIntList {

    private RangeInt[] ranges;

    public RangeIntList() {
        this.ranges = new RangeInt[0];
    }

    public RangeIntList(RangeInt range) {
        this();

        this.addRange(range);
    }

    public void addRange(RangeInt range) {
        this.ranges = Util.addToArray(this.ranges, range);
    }

    public boolean isInRange(int value) {
        for (RangeInt range : this.ranges) {
            if (range.isInRange(value)) {
                return true;
            }
        }

        return false;
    }

    public int getRangesCount() {
        return this.ranges.length;
    }

    public RangeInt getRange(int index) {
        return this.ranges[index];
    }

    @Override
    public String toString() {
        return "RangeIntList{" +
                "ranges=" + Arrays.toString(this.ranges) +
                '}';
    }
}
