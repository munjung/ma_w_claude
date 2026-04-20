package com.mahub.parser;

import com.mahub.parser.model.ServiceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceParserTest {

    private MarkdownServiceParser parser;
    private ServiceSpec spec;

    @BeforeEach
    void setUp() throws IOException {
        parser = new MarkdownServiceParser();
        try (InputStream is = getClass().getResourceAsStream("/samples/payment-service.md")) {
            Objects.requireNonNull(is, "fixture missing: /samples/payment-service.md");
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            spec = parser.parse(markdown);
        }
    }

    @Test
    void 서비스명_파싱() {
        assertThat(spec.name()).isEqualTo("payment-service");
    }

    @Test
    void published_이벤트_2개() {
        assertThat(spec.publishedEvents()).hasSize(2);
        assertThat(spec.publishedEvents())
                .extracting(e -> e.topic())
                .containsExactlyInAnyOrder("payment.completed", "payment.refunded");
    }

    @Test
    void consumed_이벤트_없음() {
        assertThat(spec.consumedEvents()).isEmpty();
    }

    @Test
    void API_3개() {
        assertThat(spec.apiEndpoints()).hasSize(3);
    }

    @Test
    void order_service_의존성() {
        assertThat(spec.dependencies())
                .extracting(d -> d.serviceName())
                .contains("order-service");
    }

    @Test
    void port_8084() {
        assertThat(spec.port()).isEqualTo(8084);
    }
}
