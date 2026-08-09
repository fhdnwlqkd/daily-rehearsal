package com.rehearsal.domain.session.port;

import com.rehearsal.domain.core.annotation.Description;
import java.io.InputStream;

@Description("세션 리허설 영상을 저장하는 storage adapter port")
public interface VideoStoragePort {

  String resolvePublicUrl(String sessionId, String originalFilename);

  void upload(String sessionId, InputStream content, String originalFilename, String contentType);
}
