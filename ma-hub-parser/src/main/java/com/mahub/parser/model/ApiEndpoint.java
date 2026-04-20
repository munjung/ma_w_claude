package com.mahub.parser.model;

import java.util.List;

public record ApiEndpoint(
        String method,
        String path,
        String summary,
        String auth,
        DtoSpec requestDto,
        DtoSpec responseDto,
        List<Integer> errors
) {}
