package pl.atakowiec.ecommerce.user.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.atakowiec.ecommerce.user.producer.event.UserCreatedEvent;
import pl.atakowiec.ecommerce.user.producer.event.UserMessageEvent;

@Component
@RequiredArgsConstructor
public class UserEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserCreatedEvent event) {
        kafkaTemplate.send(UserCreatedEvent.KEY, event.userId().toString(), event);
    }

    public void publish(UserMessageEvent event) {
        kafkaTemplate.send(UserMessageEvent.KEY, event.userId().toString(), event);
    }
}
