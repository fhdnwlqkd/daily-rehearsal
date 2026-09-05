package com.rehearsal.api.demo.controller.dto;

import com.rehearsal.domain.core.annotation.Description;

@Description("발표 데모의 Decart WebRTC 연결에 사용할 단기 client token 응답")
public record DemoDecartTokenResponse(String clientToken) {}
