package pl.atakowiec.ecommerce.user.producer.event;

import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String email,
        String firstName
) {
        public final static String KEY = "user.created";
}