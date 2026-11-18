package dev.voxelbrew.api.printer;

import static org.assertj.core.api.Assertions.*;

import dev.voxelbrew.api.model.printing.PrintProfile.Point;
import dev.voxelbrew.api.printer.driver.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;

class PrinterApiTest {
    @Test
    void configurationCopiesSecretsAtEveryBoundary() {
        char[] secret = "sensitive".toCharArray();
        var values = new HashMap<String, Object>();
        values.put("secret", secret);
        var config = new DriverConfiguration(values);
        secret[0] = 'x';
        values.clear();
        assertThat(config.requirePasswordProperty("secret")).isEqualTo("sensitive".toCharArray());
        config.requirePasswordProperty("secret")[0] = 'y';
        ((char[]) config.properties().get("secret"))[0] = 'z';
        assertThat(config.requirePasswordProperty("secret")).isEqualTo("sensitive".toCharArray());
        assertThat(config.toString()).doesNotContain("sensitive", "secret");
    }

    @Test
    void supportsOnlyExplicitTypes() {
        var values = Map.<String, Object>of(
                "text",
                "hello",
                "integer",
                4,
                "boolean",
                true,
                "decimal",
                new BigDecimal("1.25"),
                "uri",
                URI.create("https://localhost"));
        assertThat(new DriverConfiguration(values).properties()).containsExactlyInAnyOrderEntriesOf(values);
        assertThatThrownBy(() -> new DriverConfiguration(Map.of("bad", new Object())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bad");
        assertThatThrownBy(() -> new DriverConfiguration(Map.of("bad", 1.2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsEverySupportedTypeToImmutableInputWithoutChangingSecrets() {
        var config = new DriverConfiguration(Map.of(
                "text",
                "hello",
                "integer",
                4,
                "boolean",
                true,
                "decimal",
                new BigDecimal("1E+3"),
                "uri",
                URI.create("https://localhost:7125/path%20name"),
                "secret",
                "sensitive".toCharArray(),
                "emptySecret",
                new char[0]));
        var input = config.toInput();
        assertThat(input)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "text", "hello",
                        "integer", "4",
                        "boolean", "true",
                        "decimal", "1000",
                        "uri", "https://localhost:7125/path%20name",
                        "secret", "sensitive",
                        "emptySecret", ""));
        assertThatThrownBy(() -> input.put("secret", "changed")).isInstanceOf(UnsupportedOperationException.class);
        config.requirePasswordProperty("secret")[0] = 'x';
        assertThat(input).containsEntry("secret", "sensitive");
        assertThat(config.toInput()).containsEntry("secret", "sensitive");
        assertThat(config.toString()).doesNotContain("sensitive");
        assertThat(new DriverConfiguration(Map.of()).toInput()).isEmpty();
    }

    @Test
    void failuresExplicitlyDistinguishUncertainSubmissionAndCopyMessages() {
        var messages = new HashSet<>(Set.of("Connection failed"));
        var definite = new Driver.PrintResult.Failure(messages);
        var uncertain = new Driver.PrintResult.Failure(messages, true);
        messages.clear();
        assertThat(definite.submissionUncertain()).isFalse();
        assertThat(uncertain.submissionUncertain()).isTrue();
        assertThat(uncertain.messages()).containsExactly("Connection failed");
        assertThatThrownBy(() -> uncertain.messages().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void profileCopiesNestedPolygonsAndRejectsInvalidDimensions() {
        var polygon = new ArrayList<>(List.of(new Point(0, 0), new Point(100, 0), new Point(0, 100)));
        var zones = new ArrayList<List<Point>>();
        zones.add(polygon);
        var profile = profile(polygon, zones, 200);
        polygon.clear();
        zones.clear();
        assertThat(profile.bed()).hasSize(3);
        assertThat(profile.excludedZones().getFirst()).hasSize(3);
        assertThatThrownBy(() -> profile(profile.bed(), List.of(), Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile(List.of(new Point(0, 0), new Point(1, 0), new Point(2, 0)), List.of(), 200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Profile profile(List<Point> bed, List<List<Point>> zones, double height) {
        return new Profile("id", "Name", "Maker", bed, zones, height, 0.4, 1.75, 1, "marlin2", "gcode", true, null);
    }
}
