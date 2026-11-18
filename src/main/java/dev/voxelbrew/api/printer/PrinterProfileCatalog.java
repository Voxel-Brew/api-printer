package dev.voxelbrew.api.printer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Reloadable bundled and custom hardware profiles supplied by the printer runtime. */
public interface PrinterProfileCatalog {
    public record Entry(Profile definition, String image, String source, boolean bundled) {
        /** Human-readable profile name for display and sorting. */
        public String name() {
            return definition.manufacturer() + " " + definition.name();
        }
    }

    public record Result(List<Entry> entries, List<String> diagnostics) {
        public Result {
            entries = List.copyOf(entries);
            diagnostics = List.copyOf(diagnostics);
        }

        public Profile require(String id) {
            return entries.stream()
                    .filter(e -> e.definition().printer().id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing printer preset: " + id))
                    .definition();
        }
    }

    /** Saves a custom definition and copies its selected local image. Bundled IDs cannot be overwritten. */
    Profile save(Profile profile, Path image) throws IOException;

    Path directory();

    Result load();

    int exportDefaults(Set<String> selectedIds) throws IOException;

    /** Bundled illustration URL used when a printer image is unavailable. */
    String fallbackImage();
}
