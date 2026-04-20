package com.mahub.parser.model;

import java.util.List;

public record DtoSpec(
        String name,
        List<FieldSpec> fields
) {}
