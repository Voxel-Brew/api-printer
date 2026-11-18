package dev.voxelbrew.api.printer;

import dev.voxelbrew.api.model.printing.PrintArtifact;
import dev.voxelbrew.api.printer.driver.DriverConfiguration;
import java.util.Set;
import lombok.NonNull;

public record Printer(
        @NonNull String id,
        @NonNull String name,
        @NonNull String driverId,
        @NonNull DriverConfiguration driverConfiguration,
        @NonNull Profile profile) {
    public Printer {
        if (!id.matches("[a-zA-Z0-9-]{1,64}")) throw new IllegalArgumentException("Invalid printer ID");
        name = name.strip();
        if (name.isBlank() || name.length() > 128 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Printer name must contain 1?128 printable characters");
        if (driverId.isBlank()) throw new IllegalArgumentException("Driver ID is required");
    }

    /**
     * Checks a sliced artifact against this saved printer and its hardware snapshot without I/O.
     * @param artifact selected job, or null when no job has been sliced
     * @return actionable errors, or an empty set when the target metadata matches
     */
    public Set<String> validateArtifact(PrintArtifact artifact) {
        if (artifact == null) return Set.of("Slice this plate for the selected printer first");
        if (!"printer".equals(artifact.metadata().get("target_kind"))
                || !id.equals(artifact.metadata().get("target_id")))
            return Set.of(
                    "This job targets another printer or a standalone profile. Slice it for the selected printer first.");
        if (!profile.id().equals(artifact.metadata().get("target_profile")))
            return Set.of("The printer profile changed. Slice this job again.");
        if (artifact.metadata().containsKey("printer")
                && !profile.id().equals(artifact.metadata().get("printer")))
            return Set.of("Package machine differs from the configured profile. Slice again.");
        if (artifact.metadata().containsKey("nozzle_mm")
                && !Double.toString(profile.nozzleDiameter())
                        .equals(artifact.metadata().get("nozzle_mm")))
            return Set.of("Package nozzle differs from the configured printer. Slice again.");
        return Set.of();
    }
}
