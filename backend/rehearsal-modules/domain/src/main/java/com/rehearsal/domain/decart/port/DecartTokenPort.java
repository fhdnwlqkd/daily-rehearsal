package com.rehearsal.domain.decart.port;

import com.rehearsal.domain.core.annotation.Description;

@Description("Decart API에서 단기 client token을 발급받는 외부 호출 port")
public interface DecartTokenPort {

  String issueClientToken();
}
