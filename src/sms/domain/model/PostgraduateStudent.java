package sms.domain.model;

/**
 * Postgraduate student - 4.0 GPA scale, stricter 50% pass mark.
 * POLYMORPHISM: same hooks as UndergraduateStudent, completely different rules.
 */
public class PostgraduateStudent extends Student {

    private static final long serialVersionUID = 1L;

    public PostgraduateStudent(String studentId, String name, String email, String phone,
                               String department, int semester) {
        super(studentId, name, email, phone, department, semester);
    }

    @Override
    public double getPassPercentage() { return 50.0; }

    @Override
    public double getMaxGradePoint()  { return 4.0; }

    @Override
    public String classifyGrade(double p) {
        if (p >= 85) return "A";
        if (p >= 75) return "B";
        if (p >= 65) return "C";
        if (p >= getPassPercentage()) return "D";
        return "F";
    }

    @Override
    public double gradePointFor(double p) {
        if (p >= 85) return 4.0;
        if (p >= 75) return 3.0;
        if (p >= 65) return 2.0;
        if (p >= getPassPercentage()) return 1.0;
        return 0.0;
    }

    @Override
    public String getRoleLabel() { return "Postgraduate Student"; }
}
