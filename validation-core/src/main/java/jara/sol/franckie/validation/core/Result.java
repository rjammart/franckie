package jara.sol.franckie.validation.core;

import java.util.Objects;
import java.util.Optional;

public record Result(Status status, String expected, String found, Optional<String> left, Optional<String> right) {

    public enum Status {
        MATCHED,
        FAILED,
        SKIPPED
    }

    public Result {
        Objects.requireNonNull(status);
    }

    public Result(boolean matched, String expected, String found, Optional<String> left, Optional<String> right) {
        this(matched ? Status.MATCHED : Status.FAILED, expected, found, left, right);
    }

    public Result(boolean matched, String expected, String found, String left) {
        this(matched ? Status.MATCHED : Status.FAILED, expected, found, Optional.of(left), Optional.empty());
        Objects.requireNonNull(left);
    }

    public Result(boolean matched, String expected, String found) {
        this(matched ? Status.MATCHED : Status.FAILED, expected, found, Optional.empty(), Optional.empty());
    }

    public Result(Status status, String expected, String found, String left) {
        this(status, expected, found, Optional.of(left), Optional.empty());
        Objects.requireNonNull(left);
    }

    public Result(Status status, String expected, String found) {
        this(status, expected, found, Optional.empty(), Optional.empty());
    }

    public boolean matched() {
        return status == Status.MATCHED;
    }

    public boolean failed() {
        return status == Status.FAILED;
    }

    public boolean skipped() {
        return status == Status.SKIPPED;
    }
}
