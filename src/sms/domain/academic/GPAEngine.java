package sms.domain.academic;

import sms.domain.exception.InvalidAcademicRecordException;
import sms.system_services.data.DatabaseManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Calculates academic GPA and letter grades.
 * Demonstrates backend logic integration, dynamic dispatch, and exception handling.
 */
public class GPAEngine {

    public static class GradeReport {
        private final double gpa;
        private final double totalMarks;
        private final double averagePercentage;
        private final String primaryGrade;
        private final int totalCredits;

        public GradeReport(double gpa, double totalMarks, double averagePercentage, String primaryGrade, int totalCredits) {
            this.gpa = gpa;
            this.totalMarks = totalMarks;
            this.averagePercentage = averagePercentage;
            this.primaryGrade = primaryGrade;
            this.totalCredits = totalCredits;
        }

        public double getGpa() { return gpa; }
        public double getTotalMarks() { return totalMarks; }
        public double getAveragePercentage() { return averagePercentage; }
        public String getPrimaryGrade() { return primaryGrade; }
        public int getTotalCredits() { return totalCredits; }
    }

    /**
     * Calculates GPA for a student across all subjects registered in the database.
     */
    public GradeReport evaluateStudent(String studentId, boolean isPostgraduate) throws InvalidAcademicRecordException {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Map<String, Object>> marks = db.query(
                "SELECT m.mark, s.credits, s.name, s.code " +
                "FROM marks m JOIN subjects s ON m.subject_code = s.code " +
                "WHERE m.student_id = ?", studentId);

            if (marks.isEmpty()) {
                throw new InvalidAcademicRecordException("No marks recorded yet for student: " + studentId, studentId);
            }

            double totalWeightedPoints = 0.0;
            int totalCredits = 0;
            double sumMarks = 0.0;

            for (Map<String, Object> row : marks) {
                double mark = ((Number) row.get("mark")).doubleValue();
                int credits = ((Number) row.get("credits")).intValue();
                sumMarks += mark;
                totalCredits += credits;

                double gradePoint = isPostgraduate ? toPgGradePoint(mark) : toUgGradePoint(mark);
                totalWeightedPoints += (gradePoint * credits);
            }

            double gpa = totalCredits > 0 ? (totalWeightedPoints / totalCredits) : 0.0;
            double avg = sumMarks / marks.size();
            String letterGrade = toLetterGrade(avg);

            return new GradeReport(
                Math.round(gpa * 100.0) / 100.0,
                sumMarks,
                Math.round(avg * 10.0) / 10.0,
                letterGrade,
                totalCredits
            );

        } catch (SQLException e) {
            throw new InvalidAcademicRecordException("Database error during GPA evaluation: " + e.getMessage(), studentId);
        }
    }

    public static double toUgGradePoint(double mark) {
        if (mark >= 90) return 10.0;
        if (mark >= 80) return 9.0;
        if (mark >= 70) return 8.0;
        if (mark >= 60) return 7.0;
        if (mark >= 50) return 6.0;
        if (mark >= 45) return 5.0;
        return 0.0;
    }

    public static double toPgGradePoint(double mark) {
        if (mark >= 90) return 4.0;
        if (mark >= 80) return 3.5;
        if (mark >= 70) return 3.0;
        if (mark >= 60) return 2.5;
        if (mark >= 50) return 2.0;
        return 0.0;
    }

    public static String toLetterGrade(double mark) {
        if (mark >= 90) return "O (Outstanding)";
        if (mark >= 80) return "A+ (Excellent)";
        if (mark >= 70) return "A (Very Good)";
        if (mark >= 60) return "B+ (Good)";
        if (mark >= 50) return "B (Above Average)";
        if (mark >= 45) return "C (Pass)";
        return "F (Fail)";
    }
}
