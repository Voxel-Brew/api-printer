package dev.voxelbrew.api.printer.driver;

import dev.voxelbrew.api.model.vui.Layout;
import java.util.Map;

public interface DriverGui {
    /** Controls and their arrangement, interpreted by the frontend. */
    Layout layout();

    /**
     * Checks the supplied values, which may be incomplete while a host gathers input. Returns all
     * expected errors keyed by {@link dev.voxelbrew.api.model.vui.Input#id()},
     * or an empty map when valid. Each message should explain how to correct the value.
     */
    Map<String, String> validate(Map<String, String> values);

    /** Converts frontend input values, keyed by component ID, into accepted settings. */
    DriverConfiguration buildConfiguration(Map<String, String> values);
}
