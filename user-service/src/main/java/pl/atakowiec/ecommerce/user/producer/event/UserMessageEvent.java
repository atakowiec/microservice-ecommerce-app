package pl.atakowiec.ecommerce.user.producer.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserMessageEvent(
        UUID userId,
        String message,
        LocalDateTime createdAt
) {
        public final static String KEY = "user.message";
}