package com.mahub.parser.model;

import java.util.List;

public record DomainModel(
        String entityName,
        List<FieldSpec> fields
) {}
