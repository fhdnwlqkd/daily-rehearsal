package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;

public interface GetSessionVideoUploadUseCase {

  ClientSession getVideoUpload(String sessionId);
}
