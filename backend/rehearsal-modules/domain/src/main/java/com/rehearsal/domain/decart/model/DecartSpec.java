package com.rehearsal.domain.decart.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("Decart VTON WebRTC 세션에 전달할 스타일 스펙")
public record DecartSpec(String model, String prompt, String referenceImageUrl, boolean enhance) {}
