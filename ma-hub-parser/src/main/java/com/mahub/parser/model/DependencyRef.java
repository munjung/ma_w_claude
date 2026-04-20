package com.mahub.parser.model;

public record DependencyRef(
        String serviceName,
        String method,
        String path,
        String description
) {}
