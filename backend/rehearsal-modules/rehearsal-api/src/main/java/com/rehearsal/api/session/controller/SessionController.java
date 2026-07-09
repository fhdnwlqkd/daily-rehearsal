package com.rehearsal.api.session.controller;

import com.rehearsal.api.session.controller.dto.ConfirmOutfitRequest;
import com.rehearsal.api.session.controller.dto.ContextExtractionJobResponse;
import com.rehearsal.api.session.controller.dto.CreateSessionRequest;
import com.rehearsal.api.session.controller.dto.CreateSessionResponse;
import com.rehearsal.api.session.controller.dto.SessionResponse;
import com.rehearsal.api.session.controller.dto.SubmitBriefingRequest;
import com.rehearsal.api.session.controller.dto.SubmitFollowUpRequest;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final CreateSessionUseCase createSessionUseCase;
  private final UpdateClientSessionUseCase updateClientSessionUseCase;
  private final SubmitContextExtractionUseCase submitContextExtractionUseCase;
  private final GetContextExtractionUseCase getContextExtractionUseCase;

  @PostMapping
  public CreateSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
    SituationType situationType = SituationType.fromKey(request.situationType());
    ClientSession session = createSessionUseCase.createSession(situationType);
    return CreateSessionResponse.from(session);
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/briefing")
  public ContextExtractionJobResponse submitBriefing(
      @PathVariable @NotBlank String sessionId, @Valid @RequestBody SubmitBriefingRequest request) {
    ContextExtractionJob job =
        submitContextExtractionUseCase.submitBriefingExtraction(sessionId, request.transcript());
    return ContextExtractionJobResponse.from(job);
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/follow-up")
  public ContextExtractionJobResponse submitFollowUp(
      @PathVariable @NotBlank String sessionId, @Valid @RequestBody SubmitFollowUpRequest request) {
    ContextExtractionJob job =
        submitContextExtractionUseCase.submitFollowUpExtraction(sessionId, request.transcript());
    return ContextExtractionJobResponse.from(job);
  }

  @GetMapping("/{sessionId}/context-extractions/{jobId}")
  public ContextExtractionJobResponse getContextExtraction(
      @PathVariable @NotBlank String sessionId, @PathVariable @NotBlank String jobId) {
    ContextExtractionJob job = getContextExtractionUseCase.get(sessionId, jobId);
    return ContextExtractionJobResponse.from(job);
  }

  @PatchMapping("/{sessionId}/outfit")
  public SessionResponse confirmOutfit(
      @PathVariable @NotBlank String sessionId, @Valid @RequestBody ConfirmOutfitRequest request) {
    ClientSession session =
        updateClientSessionUseCase.confirmOutfit(sessionId, request.selectedOutfitId());
    return SessionResponse.from(session);
  }
}
