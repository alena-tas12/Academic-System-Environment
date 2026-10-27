package sms.system_services.rules;

import sms.domain.exception.ValidationException;
import java.util.regex.Pattern;

/**
 * Validates domain entities against academic and institutional business rules.
 * Throws specific ValidationException subclasses on non-compliance.
 */
public class ValidationEngine {

    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PHONE_PATTERN = 
            Pattern.compile("^[+0-9\\s-]{7,20}$");

    public static void validateStudent(String id, String name, String email, String phone, int semester) throws ValidationException {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Student ID cannot be empty.");
        }
        if (name == null || name.trim().length() < 2) {
            throw new ValidationException("name", "Student name must be at least 2 characters long.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("email", "Invalid email format (e.g. user@karunya.edu).");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("phone", "Invalid phone number format.");
        }
        if (semester < 1 || semester > 10) {
            throw new ValidationException("semester", "Semester must be between 1 and 10.");
        }
    }

    public static void validateMark(double mark) throws ValidationException {
        if (mark < 0.0 || mark > 100.0) {
            throw new ValidationException("mark", "Mark must be in the range [0.0 - 100.0].");
        }
    }

    public static void validateAttendance(double percentage) throws ValidationException {
        if (percentage < 0.0 || percentage > 100.0) {
            throw new ValidationException("attendance", "Attendance percentage must be between 0% and 100%.");
        }
    }
}
