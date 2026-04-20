package com.mahub.parser;

import com.mahub.parser.model.DependencyRef;
import com.mahub.parser.model.ServiceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceParserTest {

    private MarkdownServiceParser parser;
    private ServiceSpec spec;

    @BeforeEach
    void setUp() throws IOException {
        parser = new MarkdownServiceParser();
        try (InputStream is = getClass().getResourceAsStream("/samples/order-service.md")) {
            Objects.requireNonNull(is, "fixture missing: /samples/order-service.md");
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            spec = parser.parse(markdown);
        }
    }

    @Test
    void 서비스명_파싱() {
        assertThat(spec.name()).isEqualTo("order-service");
    }

    @Test
    void 도메인모델_2개_파싱() {
        assertThat(spec.domainModels()).hasSize(2);
        assertThat(spec.domainModels())
                .extracting(m -> m.entityName())
                .containsExactlyInAnyOrder("Order", "OrderItem");
    }

    @Test
    void API_엔드포인트_4개() {
        assertThat(spec.apiEndpoints()).hasSize(4);
    }

    @Test
    void user_service_의존성_파싱() {
        List<DependencyRef> deps = spec.dependencies();
        assertThat(deps).extracting(DependencyRef::serviceName)
                .contains("user-service");
    }

    @Test
    void product_service_의존성_파싱() {
        List<DependencyRef> deps = spec.dependencies();
        assertThat(deps).extracting(DependencyRef::serviceName)
                .contains("product-service");
    }

    @Test
    void published_이벤트_2개() {
        assertThat(spec.publishedEvents()).hasSize(2);
    }

    @Test
    void consumed_이벤트_파싱() {
        assertThat(spec.consumedEvents()).hasSize(1);
        assertThat(spec.consumedEvents().get(0).topic()).isEqualTo("payment.completed");
    }

    @Test
    void communication_rest_kafka() {
        assertThat(spec.communication()).isEqualTo("rest+kafka");
    }
}
