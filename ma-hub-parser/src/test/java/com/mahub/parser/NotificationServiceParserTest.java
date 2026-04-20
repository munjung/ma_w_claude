package com.mahub.parser;

import com.mahub.parser.model.ServiceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceParserTest {

    private MarkdownServiceParser parser;
    private ServiceSpec spec;

    @BeforeEach
    void setUp() throws IOException {
        parser = new MarkdownServiceParser();
        try (InputStream is = getClass().getResourceAsStream("/samples/notification-service.md")) {
            Objects.requireNonNull(is, "fixture missing: /samples/notification-service.md");
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            spec = parser.parse(markdown);
        }
    }

    @Test
    void 서비스명_파싱() {
        assertThat(spec.name()).isEqualTo("notification-service");
    }

    @Test
    void consumed_이벤트_4개() {
        assertThat(spec.consumedEvents()).hasSize(4);
        assertThat(spec.consumedEvents())
                .extracting(e -> e.topic())
                .containsExactlyInAnyOrder(
                        "user.created", "order.created",
                        "payment.completed", "order.cancelled"
                );
    }

    @Test
    void published_이벤트_없음() {
        assertThat(spec.publishedEvents()).isEmpty();
    }

    @Test
    void communication_kafka() {
        assertThat(spec.communication()).isEqualTo("kafka");
    }

    @Test
    void user_service_의존성() {
        assertThat(spec.dependencies())
                .extracting(d -> d.serviceName())
                .contains("user-service");
    }
}
