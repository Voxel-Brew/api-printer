package dev.voxelbrew.api.printer;

import dev.voxelbrew.api.i18n.TranslationService;
import dev.voxelbrew.api.model.printing.PrintArtifact;
import dev.voxelbrew.api.printer.driver.DriverConfiguration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
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
        return validateArtifact(
                artifact,
                ServiceLoader.load(TranslationService.class, TranslationService.class.getClassLoader())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("A TranslationService provider is required")));
    }

    /** Validates the artifact using the host's translation service. */
    public Set<String> validateArtifact(PrintArtifact artifact, @NonNull TranslationService translations) {
        String key = artifactError(artifact);
        return key == null ? Set.of() : localizedError(key, translations);
    }

    private Set<String> localizedError(String key, TranslationService translations) {
        return Set.of(translations.translate(key).orElseGet(() -> {
            var display = Locale.getDefault(Locale.Category.DISPLAY);
            var locale = display.getLanguage().equals("pl") ? Locale.forLanguageTag("pl") : Locale.ENGLISH;
            return ResourceBundle.getBundle("dev.voxelbrew.api.printer.messages", locale)
                    .getString(key);
        }));
    }

    private String artifactError(PrintArtifact artifact) {
        if (artifact == null) return "api.printer.artifact.missing";
        if (!"printer".equals(artifact.metadata().get("target_kind"))
                || !id.equals(artifact.metadata().get("target_id"))) return "api.printer.artifact.target";
        if (!profile.id().equals(artifact.metadata().get("target_profile"))) return "api.printer.artifact.profile";
        if (artifact.metadata().containsKey("printer")
                && !profile.id().equals(artifact.metadata().get("printer"))) return "api.printer.artifact.machine";
        if (artifact.metadata().containsKey("nozzle_mm")
                && !Double.toString(profile.nozzleDiameter())
                        .equals(artifact.metadata().get("nozzle_mm"))) return "api.printer.artifact.nozzle";
        return null;
    }
}
