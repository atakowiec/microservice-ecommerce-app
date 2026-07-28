package pl.atakowiec.ecommerce.notification.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.atakowiec.ecommerce.notification.event.UserMessageEvent;

@Component
public class UserEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    @KafkaListener(
            topics = UserMessageEvent.KEY,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleUserMessage(UserMessageEvent event) {
        log.info("Received user message: userId={}, message={}", event.userId(), event.message());
    }
}
