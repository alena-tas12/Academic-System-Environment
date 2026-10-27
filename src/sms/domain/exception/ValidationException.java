package sms.domain.exception;

/**
 * Thrown when business validation fails (e.g., invalid email, negative marks, invalid semester).
 * Demonstrates domain-specific custom exception handling.
 */
public class ValidationException extends DomainException {
    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super(String.format("Validation error on '%s': %s", fieldName, message));
        this.fieldName = fieldName;
    }

    public ValidationException(String message) {
        super(message);
        this.fieldName = "general";
    }

    public String getFieldName() {
        return fieldName;
    }
}
