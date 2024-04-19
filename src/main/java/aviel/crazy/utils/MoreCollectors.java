package aviel.crazy.utils;

import java.util.stream.Collector;

public class MoreCollectors {
    public static <Val> Collector<Val, ?, Object> ignoring() {
        return Collector.of(() -> null, (acc, v) -> {}, (acc1, acc2) -> null, acc -> null);
    }
}
