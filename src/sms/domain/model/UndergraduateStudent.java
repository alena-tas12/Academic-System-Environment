package sms.domain.model;

/**
 * Undergraduate student - 10-point GPA scale, 40% pass mark.
 * POLYMORPHISM: overrides the academic-rule hooks declared in Student.
 */
public class UndergraduateStudent extends Student {

    private static final long serialVersionUID = 1L;

    public UndergraduateStudent(String studentId, String name, String email, String phone,
                                String department, int semester) {
        super(studentId, name, email, phone, department, semester);
    }

    @Override
    public double getPassPercentage() { return 40.0; }

    @Override
    public double getMaxGradePoint()  { return 10.0; }

    @Override
    public String classifyGrade(double p) {
        if (p >= 90) return "O";
        if (p >= 80) return "A+";
        if (p >= 70) return "A";
        if (p >= 60) return "B+";
        if (p >= 50) return "B";
        if (p >= getPassPercentage()) return "C";
        return "F";
    }

    @Override
    public double gradePointFor(double p) {
        if (p >= 90) return 10.0;
        if (p >= 80) return 9.0;
        if (p >= 70) return 8.0;
        if (p >= 60) return 7.0;
        if (p >= 50) return 6.0;
        if (p >= getPassPercentage()) return 5.0;
        return 0.0;
    }

    @Override
    public String getRoleLabel() { return "Undergraduate Student"; }
}
