package kg.megalab.taskmanager.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String entityType, Object id) {
        super(ErrorCode.NOT_FOUND, entityType + " " + id + " не найден(а)");
    }

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
