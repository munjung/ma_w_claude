package com.mahub.parser;

import com.mahub.parser.model.*;

import java.util.*;

public class ServiceSpecValidator {

    private static final Set<String> VALID_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> VALID_TYPES = Set.of(
            "Long", "Integer", "String", "Boolean",
            "LocalDate", "LocalDateTime", "Double", "BigDecimal"
    );

    public List<ValidationError> validate(ServiceSpec spec) {
        List<ValidationError> errors = new ArrayList<>();

        if (spec.name() == null || spec.name().isBlank()) {
            errors.add(new ValidationError(ValidationError.Severity.ERROR, "name", "서비스명이 누락되었습니다."));
        }

        if (spec.apiEndpoints() == null || spec.apiEndpoints().isEmpty()) {
            errors.add(new ValidationError(ValidationError.Severity.WARNING, "apis", "## APIs 섹션이 없거나 엔드포인트가 정의되지 않았습니다."));
        } else {
            for (ApiEndpoint ep : spec.apiEndpoints()) {
                if (!VALID_METHODS.contains(ep.method())) {
                    errors.add(new ValidationError(
                            ValidationError.Severity.ERROR,
                            "apis." + ep.path(),
                            "유효하지 않은 HTTP 메서드: " + ep.method() + ". 허용값: " + VALID_METHODS
                    ));
                }
            }
        }

        if (spec.domainModels() != null) {
            for (DomainModel model : spec.domainModels()) {
                for (FieldSpec field : model.fields()) {
                    if (!VALID_TYPES.contains(field.type())) {
                        errors.add(new ValidationError(
                                ValidationError.Severity.WARNING,
                                model.entityName() + "." + field.name(),
                                "알 수 없는 필드 타입: " + field.type() + ". 권장 타입: " + VALID_TYPES
                        ));
                    }
                }
            }
        }

        return errors;
    }
}
