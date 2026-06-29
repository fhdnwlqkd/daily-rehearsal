package com.rehearsal.domain.decart.usecase.result;

import com.rehearsal.domain.decart.model.DecartSpec;

public record DecartSpecResult(String clientToken, DecartSpec spec) {}
