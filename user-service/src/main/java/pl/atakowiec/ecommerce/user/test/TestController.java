package pl.atakowiec.ecommerce.user.test;

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.atakowiec.ecommerce.user.producer.UserEventProducer;
import pl.atakowiec.ecommerce.user.producer.event.UserMessageEvent;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final UserEventProducer userEventProducer;

    @GetMapping("/send-message")
    public UserMessageEvent sendMessage(@Param("message") String message) {
        UserMessageEvent event = new UserMessageEvent(
                UUID.randomUUID(),
                message
        );

        userEventProducer.publish(event);
        return event;
    }
}
