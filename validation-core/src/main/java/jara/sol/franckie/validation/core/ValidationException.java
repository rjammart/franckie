package jara.sol.franckie.validation.core;

import java.util.List;
import java.util.Objects;

public class ValidationException extends RuntimeException {
    private final List<Violation> violations;

    public ValidationException(List<Violation> violations) {
        super("Validation failed with %d violation(s)".formatted(violations.size()));
        this.violations = Objects.requireNonNull(violations, "violations cannot be null");
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
