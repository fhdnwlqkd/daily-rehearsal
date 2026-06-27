package com.rehearsal.api.session.controller;

import com.rehearsal.api.session.controller.dto.CreateSessionRequest;
import com.rehearsal.api.session.controller.dto.SessionResponse;
import com.rehearsal.api.session.controller.dto.SubmitBriefingRequest;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final CreateSessionUseCase createSessionUseCase;
  private final GetSessionUseCase getSessionUseCase;
  private final UpdateClientSessionUseCase updateClientSessionUseCase;

  @PostMapping
  public SessionResponse create(@RequestBody(required = false) CreateSessionRequest request) {
    String channel = request == null ? null : request.channel();
    ClientSession session = createSessionUseCase.createSession(channel);
    return SessionResponse.from(session);
  }

  @GetMapping("/{sessionId}")
  public SessionResponse get(@PathVariable @NotBlank String sessionId) {
    ClientSession session = getSessionUseCase.getSession(sessionId);
    return SessionResponse.from(session);
  }

  @PostMapping("/{sessionId}/briefing")
  public SessionResponse submitBriefing(
      @PathVariable @NotBlank String sessionId, @Valid @RequestBody SubmitBriefingRequest request) {
    ClientSession session =
        updateClientSessionUseCase.submitBriefing(sessionId, request.transcript());
    return SessionResponse.from(session);
  }
}
