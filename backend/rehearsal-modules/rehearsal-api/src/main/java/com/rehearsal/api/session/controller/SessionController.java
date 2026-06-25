package com.rehearsal.api.session.controller;

import com.rehearsal.api.session.controller.dto.CompleteSessionContextRequest;
import com.rehearsal.api.session.controller.dto.CreateSessionRequest;
import com.rehearsal.api.session.controller.dto.SessionResponse;
import com.rehearsal.api.session.controller.dto.UpdateBriefingTranscriptRequest;
import com.rehearsal.api.session.controller.dto.UpdateFeedbackResultRequest;
import com.rehearsal.api.session.controller.dto.UpdateFinalResultRequest;
import com.rehearsal.api.session.controller.dto.UpdateSelectedOutfitRequest;
import com.rehearsal.api.session.controller.dto.UpdateSessionContextRequest;
import com.rehearsal.api.session.controller.dto.UpdateSimulationDraftRequest;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.session.usecase.command.CreateSessionCommand;
import com.rehearsal.domain.session.usecase.command.GetSessionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    ClientSession session = createSessionUseCase.createSession(new CreateSessionCommand(channel));
    return SessionResponse.from(session);
  }

  @GetMapping("/{sessionId}")
  public SessionResponse get(@PathVariable @NotBlank String sessionId) {
    ClientSession session = getSessionUseCase.getSession(new GetSessionCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/briefing-transcript")
  public SessionResponse updateBriefingTranscript(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody UpdateBriefingTranscriptRequest request) {
    ClientSession session =
        updateClientSessionUseCase.updateBriefingTranscript(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/context")
  public SessionResponse updateContext(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody UpdateSessionContextRequest request) {
    ClientSession session = updateClientSessionUseCase.updateContext(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/context/complete")
  public SessionResponse completeContext(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody CompleteSessionContextRequest request) {
    ClientSession session =
        updateClientSessionUseCase.completeContext(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/selected-outfit")
  public SessionResponse updateSelectedOutfit(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody UpdateSelectedOutfitRequest request) {
    ClientSession session =
        updateClientSessionUseCase.updateSelectedOutfit(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/simulation-draft")
  public SessionResponse updateSimulationDraft(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody UpdateSimulationDraftRequest request) {
    ClientSession session =
        updateClientSessionUseCase.updateSimulationDraft(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/feedback-result")
  public SessionResponse updateFeedbackResult(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody UpdateFeedbackResultRequest request) {
    ClientSession session =
        updateClientSessionUseCase.updateFeedbackResult(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }

  @PatchMapping("/{sessionId}/final-result")
  public SessionResponse updateFinalResult(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody UpdateFinalResultRequest request) {
    ClientSession session =
        updateClientSessionUseCase.updateFinalResult(request.toCommand(sessionId));
    return SessionResponse.from(session);
  }
}
