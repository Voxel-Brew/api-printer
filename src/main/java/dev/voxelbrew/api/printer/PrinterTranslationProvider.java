package dev.voxelbrew.api.printer;

import dev.voxelbrew.api.i18n.PropertiesTranslationProvider;
import java.util.List;
import java.util.Locale;

public final class PrinterTranslationProvider extends PropertiesTranslationProvider {
    public PrinterTranslationProvider() {
        super("dev.voxelbrew.api.printer.messages");
    }

    @Override
    protected List<Locale> getLocales() {
        return List.of("en", "pl", "fr", "it", "es", "pt", "de", "sv", "nb", "zh", "ja").stream()
                .map(Locale::forLanguageTag)
                .toList();
    }
}
