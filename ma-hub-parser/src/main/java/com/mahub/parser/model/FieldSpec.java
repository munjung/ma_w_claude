package com.mahub.parser.model;

import java.util.List;

public record FieldSpec(
        String name,
        String type,
        List<String> constraints
) {}
