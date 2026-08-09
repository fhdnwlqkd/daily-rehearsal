package com.rehearsal.api.decart.controller.dto;

import com.rehearsal.domain.core.annotation.Description;

@Description("Decart WebRTC 연결에 사용할 단기 client token 응답")
public record DecartTokenResponse(String clientToken) {}
