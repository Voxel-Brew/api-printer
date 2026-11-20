package dev.voxelbrew.api.printer;

import dev.voxelbrew.api.model.printing.PrintProfile;
import java.net.URI;
import java.util.List;
import lombok.NonNull;

/** Immutable hardware definition. Images are presentation references, never document credentials. */
public record Profile(
        @NonNull String id,
        @NonNull String name,
        @NonNull String manufacturer,
        List<PrintProfile.Point> bed,
        List<List<PrintProfile.Point>> excludedZones,
        double usableHeight,
        double nozzleDiameter,
        double filamentDiameter,
        int toolCount,
        @NonNull String firmware,
        @NonNull String outputFormat,
        boolean genericGcode,
        URI imageReference) {
    private static void dimension(double value, double maximum, String label) {
        if (!Double.isFinite(value) || value <= 0 || value > maximum)
            throw new IllegalArgumentException("Invalid " + label);
    }

    private static void validatePolygon(List<PrintProfile.Point> points) {
        if (points.size() < 3) throw new IllegalArgumentException("Polygons require at least three points");
        double area = 0;
        for (int i = 0; i < points.size(); i++) {
            var a = points.get(i);
            var b = points.get((i + 1) % points.size());
            if (Math.abs(a.x()) > 10000 || Math.abs(a.y()) > 10000)
                throw new IllegalArgumentException("Polygon exceeds supported dimensions");
            area += a.x() * b.y() - b.x() * a.y();
        }
        if (Math.abs(area) < 1e-9) throw new IllegalArgumentException("Polygon must have positive area");
    }

    public Profile {
        if (id.isBlank() || name.isBlank() || manufacturer.isBlank() || firmware.isBlank() || outputFormat.isBlank())
            throw new IllegalArgumentException("Profile identity and hardware descriptions must be nonblank");
        bed = List.copyOf(bed);
        excludedZones = excludedZones.stream().map(List::copyOf).toList();
        validatePolygon(bed);
        excludedZones.forEach(Profile::validatePolygon);
        dimension(usableHeight, 10000, "usable height");
        dimension(nozzleDiameter, 10, "nozzle diameter");
        dimension(filamentDiameter, 10, "filament diameter");
        if (toolCount < 1 || toolCount > 64) throw new IllegalArgumentException("Tool count must be between 1 and 64");
        if (genericGcode && (toolCount != 1 || !firmware.equals("marlin2") || !outputFormat.equals("gcode")))
            throw new IllegalArgumentException("Generic G-code requires one tool and text G-code output");
        if (imageReference != null && (!imageReference.isAbsolute() || imageReference.getUserInfo() != null))
            throw new IllegalArgumentException("Image reference must be absolute and contain no credentials");
    }

    public PrintProfile.Printer printer() {
        return new PrintProfile.Printer(
                id, name, nozzleDiameter, bed, excludedZones, usableHeight, firmware, outputFormat);
    }
}
