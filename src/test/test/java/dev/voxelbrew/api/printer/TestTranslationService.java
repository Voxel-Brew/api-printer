package dev.voxelbrew.api.printer;

import dev.voxelbrew.api.i18n.TranslationService;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Host with no registered messages, exercising the API's bundled message fallback. */
public final class TestTranslationService implements TranslationService {
    @Override
    public Locale currentLocale() {
        return Locale.getDefault(Locale.Category.DISPLAY);
    }

    @Override
    public Set<Locale> supportedLocales() {
        return Set.of(currentLocale());
    }

    @Override
    public Map<String, String> languages() {
        return Map.of(currentLocale().toLanguageTag(), currentLocale().getDisplayName());
    }

    @Override
    public void addSupportedLocale(Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> translate(String key, Object... arguments) {
        return Optional.empty();
    }

    @Override
    public String registerTranslation(Locale locale, String key, String text) {
        throw new UnsupportedOperationException();
    }
}
