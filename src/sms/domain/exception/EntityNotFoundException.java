package sms.domain.exception;

/**
 * Thrown when an expected domain entity (e.g. Student, Subject) cannot be found.
 */
public class EntityNotFoundException extends DomainException {
    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(String.format("%s with ID '%s' was not found in the system.", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
