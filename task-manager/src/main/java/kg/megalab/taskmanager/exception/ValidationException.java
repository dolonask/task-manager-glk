package kg.megalab.taskmanager.exception;

public class ValidationException extends ApiException {

    public ValidationException(String message, String field) {
        super(ErrorCode.VALIDATION_ERROR, message, field);
    }
}
