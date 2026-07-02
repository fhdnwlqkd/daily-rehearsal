package com.rehearsal.api.session.controller;

import com.rehearsal.api.session.controller.dto.CreateSessionRequest;
import com.rehearsal.api.session.controller.dto.CreateSessionResponse;
import com.rehearsal.api.session.controller.dto.SessionResponse;
import com.rehearsal.api.session.controller.dto.SubmitBriefingRequest;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
  private final UpdateClientSessionUseCase updateClientSessionUseCase;

  @PostMapping
  public CreateSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
    SituationType situationType = SituationType.fromKey(request.situationType());
    ClientSession session = createSessionUseCase.createSession(situationType);
    return CreateSessionResponse.from(session);
  }

  @PostMapping("/{sessionId}/briefing")
  public SessionResponse submitBriefing(
      @PathVariable @NotBlank String sessionId, @Valid @RequestBody SubmitBriefingRequest request) {
    ClientSession session =
        updateClientSessionUseCase.submitBriefing(sessionId, request.transcript());
    return SessionResponse.from(session);
  }
}
