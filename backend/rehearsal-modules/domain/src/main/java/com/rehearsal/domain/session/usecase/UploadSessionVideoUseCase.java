package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;
import java.io.InputStream;

public interface UploadSessionVideoUseCase {

  ClientSession upload(
      String sessionId, InputStream content, String originalFilename, String contentType);
}
