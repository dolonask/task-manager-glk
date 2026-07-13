package kg.megalab.taskmanager.exception;

public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}
