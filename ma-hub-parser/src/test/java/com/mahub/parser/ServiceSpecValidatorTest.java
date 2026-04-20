package com.mahub.parser;

import com.mahub.parser.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceSpecValidatorTest {

    private ServiceSpecValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ServiceSpecValidator();
    }

    @Test
    void 서비스명_누락시_ERROR() {
        ServiceSpec spec = new ServiceSpec("", "desc", List.of(), List.of(),
                List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.ERROR && e.field().equals("name"));
    }

    @Test
    void API_없을때_WARNING() {
        ServiceSpec spec = new ServiceSpec("svc", "desc", List.of(), List.of(),
                List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.WARNING && e.field().equals("apis"));
    }

    @Test
    void 잘못된_HTTP_메서드_ERROR() {
        ApiEndpoint bad = new ApiEndpoint("FETCH", "/foo", "test", "none", null, null, List.of());
        ServiceSpec spec = new ServiceSpec("svc", "desc", List.of(), List.of(bad),
                List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.ERROR && e.field().contains("/foo"));
    }

    @Test
    void 올바른_HTTP_메서드_오류없음() {
        List<ApiEndpoint> endpoints = List.of(
                new ApiEndpoint("GET", "/a", "", "none", null, null, List.of()),
                new ApiEndpoint("POST", "/b", "", "none", null, null, List.of()),
                new ApiEndpoint("PUT", "/c", "", "none", null, null, List.of()),
                new ApiEndpoint("PATCH", "/d", "", "none", null, null, List.of()),
                new ApiEndpoint("DELETE", "/e", "", "none", null, null, List.of())
        );
        ServiceSpec spec = new ServiceSpec("svc", "desc", List.of(), endpoints,
                List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).noneMatch(e -> e.severity() == ValidationError.Severity.ERROR);
    }

    @Test
    void 알수없는_필드타입_WARNING() {
        FieldSpec badField = new FieldSpec("items", "UnknownType", List.of());
        DomainModel model = new DomainModel("Order", List.of(badField));
        ServiceSpec spec = new ServiceSpec("svc", "desc", List.of(model), List.of(),
                List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.WARNING && e.message().contains("UnknownType"));
    }

    @Test
    void 알려진_필드타입_경고없음() {
        List<FieldSpec> fields = List.of(
                new FieldSpec("id", "Long", List.of()),
                new FieldSpec("name", "String", List.of()),
                new FieldSpec("amount", "BigDecimal", List.of())
        );
        DomainModel model = new DomainModel("Payment", fields);
        ApiEndpoint ep = new ApiEndpoint("GET", "/payments", "", "none", null, null, List.of());
        ServiceSpec spec = new ServiceSpec("svc", "desc", List.of(model), List.of(ep),
                List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).noneMatch(e -> e.severity() == ValidationError.Severity.ERROR);
    }

    @Test
    void 정상_스펙_오류없음() {
        ApiEndpoint ep = new ApiEndpoint("GET", "/users", "", "none", null, null, List.of());
        ServiceSpec spec = new ServiceSpec("user-service", "설명", List.of(), List.of(ep),
                List.of(), List.of(), List.of(), "rest", "postgresql", 8081, "/user-service");
        List<ValidationError> errors = validator.validate(spec);
        assertThat(errors).isEmpty();
    }
}
