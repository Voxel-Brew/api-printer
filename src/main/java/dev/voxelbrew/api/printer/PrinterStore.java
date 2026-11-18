package dev.voxelbrew.api.printer;

import java.io.IOException;
import java.util.List;

/**
 * Versioned physical-printer persistence. Credentials never belong in model documents.
 */
public interface PrinterStore {
    record Snapshot(List<Printer> devices, List<String> diagnostics) {
        public Snapshot {
            devices = List.copyOf(devices);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    Snapshot load() throws IOException;

    void save(Printer printer) throws IOException;

    void remove(String id) throws IOException;
}
