package com.mahub.parser.model;

import java.util.List;

public record ServiceSpec(
        String name,
        String description,
        List<DomainModel> domainModels,
        List<ApiEndpoint> apiEndpoints,
        List<DependencyRef> dependencies,
        List<EventSpec> publishedEvents,
        List<EventSpec> consumedEvents,
        String communication,
        String dbType,
        int port,
        String contextPath
) {}
