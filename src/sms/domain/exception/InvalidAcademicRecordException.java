package sms.domain.exception;

/**
 * Checked exception thrown when an academic record calculation or constraint is violated.
 * Demonstrates checked exception handling with explicit throws and try-catch requirements.
 */
public class InvalidAcademicRecordException extends Exception {
    private final String recordId;

    public InvalidAcademicRecordException(String message, String recordId) {
        super(message);
        this.recordId = recordId;
    }

    public InvalidAcademicRecordException(String message) {
        super(message);
        this.recordId = "N/A";
    }

    public String getRecordId() { return recordId; }
}
