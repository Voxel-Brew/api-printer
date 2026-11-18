package dev.voxelbrew.api.printer.driver;

import dev.voxelbrew.api.model.printing.PrintArtifact;
import java.util.Set;

/** Stateless device integration. Hosts serialize operations per physical printer on worker threads.
 * Each operation owns and closes its transports. Metadata and form creation must not connect. */
public interface Driver {
    String id();

    /** Human-readable integration name used by hosts for driver selection. */
    String name();

    /** Validates, transfers, and submits an artifact. Success means submission, not completion.
     * @param configuration saved connection settings
     * @param artifact verified job bytes and provenance
     * @return submission acceptance or failure, marking uncertain submission explicitly; never retry automatically
     * @throws Exception on unexpected integration failures not represented by a result
     */
    PrintResult print(DriverConfiguration configuration, PrintArtifact artifact) throws Exception;

    /** Reads current device state with operation-owned transports.
     * @param configuration saved connection settings
     * @return current state; unavailable measurements remain empty
     * @throws Exception if monitoring fails or the response is invalid
     */
    DriverStatus readPrinterStatus(DriverConfiguration configuration) throws Exception;

    DriverGui gui();

    sealed interface PrintResult {
        enum Success implements PrintResult {
            SUCCESS
        }

        /** Failed operation. Uncertain submission requires checking the device before another attempt. */
        record Failure(Set<String> messages, boolean submissionUncertain) implements PrintResult {
            public Failure(Set<String> messages) {
                this(messages, false);
            }

            public Failure {
                messages = Set.copyOf(messages);
                if (messages.isEmpty()) {
                    throw new IllegalArgumentException("Failure messages cannot be empty");
                }
            }
        }
    }
}
