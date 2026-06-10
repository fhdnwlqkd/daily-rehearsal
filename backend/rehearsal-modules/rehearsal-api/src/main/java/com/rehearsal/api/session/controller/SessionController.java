package com.rehearsal.api.session.controller;

import com.rehearsal.api.session.application.SessionService;
import com.rehearsal.api.session.contract.SessionContract.CreateSessionCommand;
import com.rehearsal.api.session.contract.SessionContract.CreateSessionResult;
import com.rehearsal.api.session.contract.SessionContract.GetSessionResult;
import com.rehearsal.api.session.controller.dto.SessionDto.CreateSessionRequest;
import com.rehearsal.api.session.controller.dto.SessionDto.CreateSessionResponse;
import com.rehearsal.api.session.controller.dto.SessionDto.GetSessionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

  private final SessionService sessionService;

  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @PostMapping
  public CreateSessionResponse create(@RequestBody(required = false) CreateSessionRequest request) {
    String channel = request == null ? null : request.channel();
    CreateSessionResult result = sessionService.create(new CreateSessionCommand(channel));
    return toCreateSessionResponse(result);
  }

  @GetMapping("/{sessionId}")
  public GetSessionResponse get(@PathVariable String sessionId) {
    GetSessionResult result = sessionService.get(sessionId);
    return toGetSessionResponse(result);
  }

  private CreateSessionResponse toCreateSessionResponse(CreateSessionResult result) {
    return new CreateSessionResponse(
        result.sessionId(),
        result.channel(),
        result.status(),
        result.contextStatus(),
        result.followUpAttempt(),
        result.briefingTranscript(),
        result.partialContext(),
        result.finalUserContext(),
        result.missingRequiredSlotKeys(),
        result.followUpQuestion(),
        result.selectedOutfitId(),
        result.simulationDraft(),
        result.feedbackResult(),
        result.finalResult());
  }

  private GetSessionResponse toGetSessionResponse(GetSessionResult result) {
    return new GetSessionResponse(
        result.sessionId(),
        result.channel(),
        result.status(),
        result.contextStatus(),
        result.followUpAttempt(),
        result.briefingTranscript(),
        result.partialContext(),
        result.finalUserContext(),
        result.missingRequiredSlotKeys(),
        result.followUpQuestion(),
        result.selectedOutfitId(),
        result.simulationDraft(),
        result.feedbackResult(),
        result.finalResult());
  }
}
