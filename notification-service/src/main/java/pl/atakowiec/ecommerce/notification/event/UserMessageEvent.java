package pl.atakowiec.ecommerce.notification.event;

import java.util.UUID;

public record UserMessageEvent(
        UUID userId,
        String message
) {
    public final static String KEY = "user.message";
}