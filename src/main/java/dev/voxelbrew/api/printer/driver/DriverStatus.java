package dev.voxelbrew.api.printer.driver;

import java.util.OptionalDouble;
import lombok.NonNull;

/** Current device observation; missing measurements do not imply zero. Temperatures use Celsius. */
public record DriverStatus(
        @NonNull State state,
        @NonNull String filename,
        @NonNull OptionalDouble progress,
        @NonNull OptionalDouble nozzle,
        @NonNull OptionalDouble bed,
        @NonNull OptionalDouble remainingSeconds,
        @NonNull String detail) {
    public enum State {
        OFFLINE,
        UNKNOWN,
        IDLE,
        PRINTING,
        PAUSED,
        COMPLETE,
        ERROR
    }

    public static DriverStatus unknown(String detail) {
        return new DriverStatus(
                State.UNKNOWN,
                "",
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                detail);
    }

    public boolean canStart() {
        return state == State.IDLE || state == State.COMPLETE;
    }
}
