package dev.voxelbrew.api.printer;

import dev.voxelbrew.api.model.CadDocument;
import dev.voxelbrew.api.model.printing.PrintArtifact;
import java.util.*;

/** Shared compatibility rules, usable by headless clients and UI. */
@lombok.experimental.UtilityClass
public class PrinterJobs {

    public static Optional<String> incompatibility(CadDocument document, String plate, Printer device) {
        if (document.gcode().getOrDefault(plate, List.of()).isEmpty()) return Optional.of("Slice this plate first");
        PrintArtifact artifact = document.printArtifacts().get(plate);
        if (artifact == null || !"printer".equals(artifact.metadata().get("target_kind")))
            return Optional.of("This plate was sliced for a printer profile; save its G-code instead");
        if (!device.id().equals(artifact.metadata().get("target_id")))
            return Optional.of("This job was sliced for another printer");
        if (!device.profileId().equals(artifact.metadata().get("target_profile")))
            return Optional.of("Printer profile changed since slicing");
        if (device.kind() == Printer.Kind.BAMBU) {
            if (artifact == null
                    || !artifact.format().equals("gcode.3mf")
                    || !"orca".equals(artifact.metadata().get("backend")))
                return Optional.of("Bambu requires an OrcaSlicer package; slice using the matching machine preset");
            if (!normalize(device.model()).equals(normalize(artifact.metadata().getOrDefault("model", ""))))
                return Optional.of("Sliced machine model differs from the selected device");
            if (!"0".equals(artifact.metadata().get("tool")))
                return Optional.of("Only single-material tool 0 jobs are supported");
            double slicedNozzle = document.printSetup().printerSnapshot() == null
                    ? 0.4
                    : document.printSetup().printerSnapshot().printer().nozzleMm();
            if (!Double.toString(slicedNozzle).equals(artifact.metadata().get("nozzle_mm")))
                return Optional.of("Package nozzle differs from the sliced printer profile");
        } else if (artifact != null && artifact.format().equals("gcode.3mf"))
            return Optional.of("A Bambu job cannot be sent to Moonraker");
        if (!device.profileId().equals(document.printSetup().printerId()))
            return Optional.of("Current printer profile differs from the sliced job");
        return Optional.empty();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace("bambu lab", "")
                .replace("bambu-lab-", "")
                .replaceAll("[^a-z0-9]", "");
    }
}
