package dev.voxelbrew.api.printer.driver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

public interface DriverRegistry extends AutoCloseable {
    @Override
    void close() throws IOException;

    DriverRegistry withDirectory(Path directory) throws IOException;

    @NonNull
    List<Driver> drivers();

    @NonNull
    Optional<Driver> find(@NonNull String id);
}
