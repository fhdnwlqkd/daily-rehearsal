package com.rehearsal.api.decart.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.port.DecartTokenPort;
import com.rehearsal.domain.decart.usecase.GetOutfitSpecUseCase;
import com.rehearsal.domain.decart.usecase.IssueDecartTokenUseCase;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description("Decart client token 발급 및 outfit 스펙 조회를 처리하는 application service")
@Service
@RequiredArgsConstructor
public class DecartSpecService implements IssueDecartTokenUseCase, GetOutfitSpecUseCase {

  private final SessionCache sessionCache;
  private final DecartTokenPort decartTokenPort;
  private final OutfitSpecResolver outfitSpecResolver;

  @Override
  public String issueDecartToken(String sessionId) {
    ClientSession session = getValidSession(sessionId);
    validateSessionStatus(session);
    return decartTokenPort.issueClientToken();
  }

  @Override
  public DecartSpec getOutfitSpec(String sessionId, String outfitId) {
    ClientSession session = getValidSession(sessionId);
    validateSessionStatus(session);
    session.updateSelectedOutfitId(outfitId);
    sessionCache.save(session);
    return outfitSpecResolver.resolve(outfitId);
  }

  private ClientSession getValidSession(String sessionId) {
    return sessionCache
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  private void validateSessionStatus(ClientSession session) {
    if (session.getStatus() != SessionStatus.TRANSFORMATION_READY) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
