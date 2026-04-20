package com.mahub.parser;

import com.mahub.parser.model.ServiceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceParserTest {

    private MarkdownServiceParser parser;
    private ServiceSpec spec;

    @BeforeEach
    void setUp() throws IOException {
        parser = new MarkdownServiceParser();
        try (InputStream is = getClass().getResourceAsStream("/samples/product-service.md")) {
            Objects.requireNonNull(is, "fixture missing: /samples/product-service.md");
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            spec = parser.parse(markdown);
        }
    }

    @Test
    void 서비스명_파싱() {
        assertThat(spec.name()).isEqualTo("product-service");
    }

    @Test
    void 도메인모델_Product_필드_5개() {
        assertThat(spec.domainModels()).hasSize(1);
        assertThat(spec.domainModels().get(0).fields()).hasSize(5);
    }

    @Test
    void API_3개() {
        assertThat(spec.apiEndpoints()).hasSize(3);
    }

    @Test
    void auth_api_key_포함() {
        assertThat(spec.apiEndpoints())
                .extracting(ep -> ep.auth())
                .contains("api-key");
    }

    @Test
    void consumed_이벤트_order_created() {
        assertThat(spec.consumedEvents()).hasSize(1);
        assertThat(spec.consumedEvents().get(0).topic()).isEqualTo("order.created");
    }

    @Test
    void port_8083() {
        assertThat(spec.port()).isEqualTo(8083);
    }
}
