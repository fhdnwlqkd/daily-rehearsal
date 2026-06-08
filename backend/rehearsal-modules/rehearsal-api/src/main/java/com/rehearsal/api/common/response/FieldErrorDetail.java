package com.rehearsal.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorDetail(String field, String reason, Object rejectedValue) {}
