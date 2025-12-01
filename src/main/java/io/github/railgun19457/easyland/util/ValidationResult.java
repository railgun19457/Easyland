package io.github.railgun19457.easyland.util;

import org.jetbrains.annotations.Nullable;

/**
 * A modern, sealed validation result type.
 * This allows for exhaustive checks and type-safe handling of validation outcomes.
 */
public sealed interface ValidationResult {
    /**
     * Represents a successful validation.
     */
    record Success() implements ValidationResult {
    }

    /**
     * Represents a failed validation, containing an error message.
     * @param errorMessage A message describing the validation failure.
     */
    record Failure(@Nullable String errorMessage) implements ValidationResult {
    }
}