package sms.domain.model;

/** Lifecycle states of a student - the state machine that drives business rules. */
public enum StudentStatus {
    APPLIED, ADMITTED, ACTIVE, ON_LEAVE, COMPLETED, GRADUATED, WITHDRAWN, ARCHIVED
}
