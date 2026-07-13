package kg.megalab.taskmanager.security;

import kg.megalab.taskmanager.exception.ErrorCode;
import kg.megalab.taskmanager.exception.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Требуется аутентификация");
        }
        return principal;
    }
}
