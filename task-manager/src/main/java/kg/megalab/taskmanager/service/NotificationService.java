package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Notification;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.notification.NotificationResponse;
import kg.megalab.taskmanager.exception.ForbiddenException;
import kg.megalab.taskmanager.exception.NotFoundException;
import kg.megalab.taskmanager.repository.NotificationRepository;
import kg.megalab.taskmanager.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(User recipient, String message, String entityType, UUID entityId) {
        if (recipient == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForCurrentUser(boolean unreadOnly) {
        UUID userId = SecurityUtils.currentUser().id();
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::toResponse).toList();
    }

    public void markRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification", id));
        if (!notification.getRecipient().getId().equals(SecurityUtils.currentUser().id())) {
            throw new ForbiddenException("Уведомление принадлежит другому пользователю");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getEntityType(), n.getEntityId(),
                n.isRead(), n.getCreatedAt());
    }
}
