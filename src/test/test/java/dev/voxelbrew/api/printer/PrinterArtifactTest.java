package dev.voxelbrew.api.printer;

import static org.assertj.core.api.Assertions.*;

import dev.voxelbrew.api.model.printing.*;
import dev.voxelbrew.api.printer.driver.DriverConfiguration;
import java.util.*;
import org.junit.jupiter.api.Test;

class PrinterArtifactTest {
    private java.util.Locale previousDisplayLocale;

    @org.junit.jupiter.api.BeforeEach
    void selectEnglishForExpectedMessages() {
        previousDisplayLocale = java.util.Locale.getDefault(java.util.Locale.Category.DISPLAY);
        java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, java.util.Locale.ENGLISH);
    }

    @org.junit.jupiter.api.AfterEach
    void restoreDisplayLocale() {
        java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, previousDisplayLocale);
    }

    private Printer printer() {
        var hardware = PrintProfile.resolve(PrintSetup.empty()).printer();
        var profile = new Profile(
                hardware.id(),
                hardware.name(),
                "Generic",
                hardware.bed(),
                hardware.excludedZones(),
                hardware.usableHeight(),
                hardware.nozzleMm(),
                1.75,
                1,
                hardware.firmware(),
                hardware.outputFormat(),
                true,
                null);
        return new Printer("target", "Target", "fixture", new DriverConfiguration(Map.of()), profile);
    }

    private PrintArtifact artifact(Printer printer, Map<String, String> changes) {
        var metadata = new HashMap<>(Map.of(
                "target_kind",
                "printer",
                "target_id",
                printer.id(),
                "target_profile",
                printer.profile().id()));
        metadata.putAll(changes);
        return PrintArtifact.of("gcode", new byte[] {1}, metadata);
    }

    @Test
    void validatesMissingProfileOnlyAndExactPrinterTargetsWithoutIo() {
        var printer = printer();
        assertThat(printer.validateArtifact(null)).containsExactly("Slice this plate for the selected printer first");
        assertThat(printer.validateArtifact(artifact(printer, Map.of("target_kind", "profile"))))
                .anySatisfy(message -> assertThat(message).contains("standalone profile"));
        assertThat(printer.validateArtifact(artifact(printer, Map.of("target_id", "other"))))
                .anySatisfy(message -> assertThat(message).contains("another printer"));
        assertThat(printer.validateArtifact(artifact(printer, Map.of()))).isEmpty();
    }

    @Test
    void validatesSavedProfileAndOptionalPackageMachineAndNozzle() {
        var printer = printer();
        assertThat(printer.validateArtifact(artifact(printer, Map.of("target_profile", "changed"))))
                .anySatisfy(message -> assertThat(message).contains("profile changed"));
        assertThat(printer.validateArtifact(artifact(printer, Map.of("printer", "other"))))
                .anySatisfy(message -> assertThat(message).contains("Package machine"));
        assertThat(printer.validateArtifact(artifact(printer, Map.of("nozzle_mm", "0.8"))))
                .anySatisfy(message -> assertThat(message).contains("Package nozzle"));
        assertThat(printer.validateArtifact(artifact(
                        printer,
                        Map.of(
                                "printer",
                                printer.profile().id(),
                                "nozzle_mm",
                                Double.toString(printer.profile().nozzleDiameter())))))
                .isEmpty();
    }
}
