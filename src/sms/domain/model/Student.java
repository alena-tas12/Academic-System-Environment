package sms.domain.model;

import sms.domain.exception.DomainException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Abstract Student - carries the LIFECYCLE STATE MACHINE (APPLIED -> ... -> GRADUATED)
 * and declares POLYMORPHIC academic rules (grade scale differs per student type).
 */
public abstract class Student extends Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private String department;
    private int semester;
    private StudentStatus status;
    private final LocalDate admissionDate;

    // Legal lifecycle transitions: state creates rules.
    private static final Map<StudentStatus, StudentStatus[]> TRANSITIONS = new EnumMap<>(StudentStatus.class);
    static {
        TRANSITIONS.put(StudentStatus.APPLIED,    new StudentStatus[]{StudentStatus.ADMITTED, StudentStatus.WITHDRAWN, StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.ADMITTED,   new StudentStatus[]{StudentStatus.ACTIVE, StudentStatus.WITHDRAWN, StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.ACTIVE,     new StudentStatus[]{StudentStatus.ON_LEAVE, StudentStatus.COMPLETED, StudentStatus.WITHDRAWN, StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.ON_LEAVE,   new StudentStatus[]{StudentStatus.ACTIVE, StudentStatus.WITHDRAWN, StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.COMPLETED,  new StudentStatus[]{StudentStatus.GRADUATED, StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.GRADUATED,  new StudentStatus[]{StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.WITHDRAWN,  new StudentStatus[]{StudentStatus.ARCHIVED});
        TRANSITIONS.put(StudentStatus.ARCHIVED,   new StudentStatus[]{});
    }

    protected Student(String studentId, String name, String email, String phone,
                      String department, int semester) {
        super(studentId, name, email, phone);
        this.department = department;
        this.semester = semester;
        this.status = StudentStatus.ADMITTED;
        this.admissionDate = LocalDate.now();
    }

    // ---------- Lifecycle rules (state -> behaviour) ----------

    public boolean canTransitionTo(StudentStatus next) {
        for (StudentStatus s : TRANSITIONS.get(status)) {
            if (s == next) return true;
        }
        return false;
    }

    public void transitionTo(StudentStatus next) {
        if (!canTransitionTo(next)) {
            throw new DomainException("Illegal lifecycle transition: " + status + " -> " + next);
        }
        this.status = next;
    }

    /** Business rule examples from the blueprint. */
    public boolean canReceiveAttendance() {
        return status == StudentStatus.ACTIVE || status == StudentStatus.ADMITTED;
    }

    public boolean canEnroll() {
        return status == StudentStatus.ACTIVE || status == StudentStatus.ADMITTED;
    }

    public boolean isArchived() { return status == StudentStatus.ARCHIVED; }

    // ---------- Polymorphic academic rules ----------

    /** Minimum percentage required to pass, differs UG vs PG. */
    public abstract double getPassPercentage();

    /** Top of the grade-point scale (UG: 10-point, PG: 4-point). */
    public abstract double getMaxGradePoint();

    /** Letter grade for a percentage, scale depends on student type. */
    public abstract String classifyGrade(double percentage);

    /** Grade points for a percentage, scale depends on student type. */
    public abstract double gradePointFor(double percentage);

    // ---------- Accessors ----------

    public String getDepartment()          { return department; }
    public int getSemester()               { return semester; }
    public StudentStatus getStatus()       { return status; }
    public LocalDate getAdmissionDate()    { return admissionDate; }

    public void setDepartment(String department) { this.department = department; }
    public void setSemester(int semester)        { this.semester = semester; }

    @Override
    public String getRoleLabel() { return "Student"; }
}
