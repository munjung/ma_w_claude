package com.mahub.parser;

import com.mahub.parser.model.ServiceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceParserTest {

    private MarkdownServiceParser parser;
    private ServiceSpec spec;

    @BeforeEach
    void setUp() throws IOException {
        parser = new MarkdownServiceParser();
        try (InputStream is = getClass().getResourceAsStream("/samples/user-service.md")) {
            Objects.requireNonNull(is, "fixture missing: /samples/user-service.md");
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            spec = parser.parse(markdown);
        }
    }

    @Test
    void 서비스명_파싱() {
        assertThat(spec.name()).isEqualTo("user-service");
    }

    @Test
    void description_파싱() {
        assertThat(spec.description()).contains("사용자 계정");
    }

    @Test
    void 도메인모델_파싱() {
        assertThat(spec.domainModels()).hasSize(1);
        assertThat(spec.domainModels().get(0).entityName()).isEqualTo("User");
        assertThat(spec.domainModels().get(0).fields()).hasSize(5);
    }

    @Test
    void API_엔드포인트_개수() {
        assertThat(spec.apiEndpoints()).hasSize(3);
    }

    @Test
    void API_메서드_파싱() {
        assertThat(spec.apiEndpoints())
                .extracting(ep -> ep.method())
                .containsExactlyInAnyOrder("POST", "GET", "PUT");
    }

    @Test
    void published_이벤트_파싱() {
        assertThat(spec.publishedEvents()).hasSize(1);
        assertThat(spec.publishedEvents().get(0).topic()).isEqualTo("user.created");
    }

    @Test
    void port_파싱() {
        assertThat(spec.port()).isEqualTo(8081);
    }

    @Test
    void contextPath_파싱() {
        assertThat(spec.contextPath()).isEqualTo("/user-service");
    }
}
