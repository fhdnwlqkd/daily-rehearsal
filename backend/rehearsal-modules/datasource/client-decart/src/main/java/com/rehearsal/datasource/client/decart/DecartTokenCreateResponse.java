package com.rehearsal.datasource.client.decart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecartTokenCreateResponse(@JsonProperty("apiKey") String apiKey) {}
