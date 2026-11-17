package dev.voxelbrew.api.printer;

import static org.assertj.core.api.Assertions.*;

import dev.voxelbrew.api.model.CadDocument;
import dev.voxelbrew.api.model.printing.PrintArtifact;
import dev.voxelbrew.api.model.printing.PrintTarget;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrinterApiTest {
    @TempDir
    Path directory;

    @Test
    void printerNameIsTheOnlyUserFacingIdentity() {
        var printer = new Printer(
                "internal-uuid",
                "  Lab Klipper  ",
                Printer.Kind.MOONRAKER,
                "generic-marlin-200-0.4",
                "http://localhost:7125",
                "",
                "",
                "");
        assertThat(printer.name()).isEqualTo("Lab Klipper");
        assertThat(printer.toString()).isEqualTo("Lab Klipper");
        assertThat(printer.id()).isEqualTo("internal-uuid");
    }

    @Test
    void deviceAddressesAreValidatedBeforeSaving() {
        for (var address : List.of(
                "moonraker.local",
                "ftp://moonraker.local",
                "http://user@moonraker.local",
                "http://moonraker.local:0",
                "http://moonraker.local:65536",
                "http://moonraker.local?key=secret",
                "http://moonraker.local:abc",
                "http://moonraker.local:7125/ bad")) {
            assertThatThrownBy(() ->
                            new Printer("device", "Device", Printer.Kind.MOONRAKER, "profile", address, "", "", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(new Printer(
                                "device",
                                "Device",
                                Printer.Kind.MOONRAKER,
                                "profile",
                                " https://moonraker.local:7125 ",
                                "",
                                "",
                                "")
                        .address())
                .isEqualTo("https://moonraker.local:7125");
        for (var address : List.of(
                "256.1.1.1",
                "192.168.1",
                "printer..local",
                "-printer.local",
                "printer.local:8883",
                "http://printer.local",
                "printer local")) {
            assertThatThrownBy(() -> new Printer(
                            "device", "Device", Printer.Kind.BAMBU, "profile", address, "serial", "model", "code"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (var address : List.of("192.168.1.25", "printer.local", "localhost")) {
            assertThat(new Printer(
                                    "device",
                                    "Device",
                                    Printer.Kind.BAMBU,
                                    "profile",
                                    address,
                                    "serial",
                                    "model",
                                    "code")
                            .address())
                    .isEqualTo(address);
        }
    }

    @Test
    void genericGcodeCannotMasqueradeAsBambuPackage() {
        var doc = new CadDocument("test", List.of(), 0).withGcode(Map.of("plate", List.of("G1 X1")));
        var device = new Printer(
                "a1",
                "A1",
                Printer.Kind.BAMBU,
                "generic-marlin-200-0.4",
                "localhost",
                "serial",
                "Bambu Lab A1",
                "code");
        assertThat(PrinterJobs.incompatibility(doc, "plate", device).isPresent())
                .isTrue();
        var artifact = PrintArtifact.of(
                "gcode.3mf",
                new byte[] {1, 2, 3},
                Map.of(
                        "backend",
                        "orca",
                        "model",
                        "Bambu Lab A1",
                        "tool",
                        "0",
                        "nozzle_mm",
                        "0.4",
                        "target_kind",
                        "printer",
                        "target_id",
                        "a1",
                        "target_profile",
                        "generic-marlin-200-0.4"));
        doc = doc.withPrintArtifacts(Map.of("plate", artifact));
        assertThat(PrinterJobs.incompatibility(doc, "plate", device).isEmpty()).isTrue();
        var widerNozzle = new CadDocument("test", List.of(), 0, doc.printSetup().withNozzle(0.6))
                .withGcode(Map.of("plate", List.of("G1 X1")))
                .withPrintArtifacts(Map.of(
                        "plate",
                        PrintArtifact.of(
                                "gcode.3mf",
                                new byte[] {1, 2, 3},
                                Map.of(
                                        "backend",
                                        "orca",
                                        "model",
                                        "Bambu Lab A1",
                                        "tool",
                                        "0",
                                        "nozzle_mm",
                                        "0.6",
                                        "target_kind",
                                        "printer",
                                        "target_id",
                                        "a1",
                                        "target_profile",
                                        "generic-marlin-200-0.4"))));
        assertThat(PrinterJobs.incompatibility(widerNozzle, "plate", device).isEmpty())
                .isTrue();
        assertThat(PrinterJobs.incompatibility(
                                widerNozzle.withPrintArtifacts(Map.of("plate", artifact)), "plate", device)
                        .isPresent())
                .isTrue();
        assertThat(PrinterJobs.incompatibility(
                                doc,
                                "plate",
                                new Printer(
                                        "other",
                                        "Other",
                                        Printer.Kind.BAMBU,
                                        "generic-marlin-200-0.4",
                                        "localhost",
                                        "serial",
                                        "P1S",
                                        "code"))
                        .isPresent())
                .isTrue();
        assertThat(PrinterJobs.incompatibility(doc, "missing", device).isPresent())
                .isTrue();
    }

    @Test
    void onlyTheExactSlicedPrinterCanReceiveAJob() {
        var document = CadDocument.empty();
        var plate = document.printSetup().activePlateId();
        var lines = List.of("G21", "G1 X1 E1");
        var printer = new Printer(
                "workshop",
                "Workshop",
                Printer.Kind.MOONRAKER,
                document.printSetup().printerId(),
                "http://localhost:7125",
                "",
                "",
                "");
        var profileJob = PrintTarget.attach(null, lines, document.printSetup());
        var saved = document.withGcode(Map.of(plate, lines)).withPrintArtifacts(Map.of(plate, profileJob));
        assertThat(PrinterJobs.incompatibility(saved, plate, printer).isPresent())
                .isTrue();

        var connected = document.printSetup().withPrintTarget(printer.id());
        var printerJob = PrintTarget.attach(null, lines, connected);
        saved = saved.withPrintArtifacts(Map.of(plate, printerJob));
        assertThat(PrinterJobs.incompatibility(saved, plate, printer).isEmpty()).isTrue();
        var other = new Printer(
                "other", "Other", Printer.Kind.MOONRAKER, printer.profileId(), "http://localhost:7126", "", "", "");
        assertThat(PrinterJobs.incompatibility(saved, plate, other).isPresent()).isTrue();
    }

    @Test
    void independentJavaConsumerCompilesWithoutApplicationModules() throws Exception {
        Path source = directory.resolve("Consumer.java");
        Files.writeString(
                source,
                "import dev.voxelbrew.api.printer.*; public class Consumer { public PrinterStatus.State state(PrinterSession session) throws java.io.IOException { return session.status().state(); } }");
        assertThat(javax.tools.ToolProvider.getSystemJavaCompiler()
                        .run(
                                null,
                                null,
                                null,
                                "-source",
                                "21",
                                "-target",
                                "21",
                                "-classpath",
                                System.getProperty("java.class.path"),
                                source.toString()))
                .isEqualTo(0);
    }
}
