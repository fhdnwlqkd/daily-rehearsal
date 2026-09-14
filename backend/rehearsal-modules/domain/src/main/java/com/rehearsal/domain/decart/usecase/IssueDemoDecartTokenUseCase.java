package com.rehearsal.domain.decart.usecase;

import com.rehearsal.domain.core.annotation.Description;

@Description("발표 데모용 Decart client token 발급 use case")
public interface IssueDemoDecartTokenUseCase {

  String issueDemoDecartToken();
}
