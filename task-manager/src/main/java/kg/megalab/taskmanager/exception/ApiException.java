package kg.megalab.taskmanager.exception;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final String field;

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
