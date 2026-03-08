package jara.sol.franckie.validation.core;

import java.util.Map;
import java.util.Objects;

public record Violation(String translationKey, Map<String, String> arguments, Result result) {
    public Violation {
        Objects.requireNonNull(translationKey);
        Objects.requireNonNull(arguments);
        Objects.requireNonNull(result);
    }

    public static Violation of(String translationKey, Map<String, String> arguments, Result result) {
        return new Violation(translationKey, arguments, result);
    }
}
