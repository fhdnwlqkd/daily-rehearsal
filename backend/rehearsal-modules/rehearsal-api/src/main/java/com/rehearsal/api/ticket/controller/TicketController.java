package com.rehearsal.api.ticket.controller;

import com.rehearsal.api.ticket.controller.dto.TicketJobResponse;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.usecase.GetTicketGenerationUseCase;
import com.rehearsal.domain.ticket.usecase.SubmitTicketGenerationUseCase;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class TicketController {

  private final SubmitTicketGenerationUseCase submitTicketGenerationUseCase;
  private final GetTicketGenerationUseCase getTicketGenerationUseCase;

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/ticket")
  public TicketJobResponse submitTicket(@PathVariable @NotBlank String sessionId) {
    TicketJob job = submitTicketGenerationUseCase.submit(sessionId);
    return TicketJobResponse.from(job);
  }

  @GetMapping("/{sessionId}/ticket")
  public TicketJobResponse getTicket(@PathVariable @NotBlank String sessionId) {
    TicketJob job = getTicketGenerationUseCase.get(sessionId);
    return TicketJobResponse.from(job);
  }
}
