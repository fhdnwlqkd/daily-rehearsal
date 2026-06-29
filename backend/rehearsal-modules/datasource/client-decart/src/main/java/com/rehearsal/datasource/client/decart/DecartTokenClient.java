package com.rehearsal.datasource.client.decart;

import com.rehearsal.domain.core.annotation.Description;

@Description("Decart token 발급 HTTP 호출을 추상화하는 client 인터페이스")
public interface DecartTokenClient {

  DecartTokenCreateResponse createToken();
}
