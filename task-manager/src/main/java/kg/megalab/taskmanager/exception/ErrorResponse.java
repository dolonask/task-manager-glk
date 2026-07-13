package kg.megalab.taskmanager.exception;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, String field) {
    }

    public static ErrorResponse of(ErrorCode code, String message, String field) {
        return new ErrorResponse(new ErrorBody(code.name(), message, field));
    }
}
