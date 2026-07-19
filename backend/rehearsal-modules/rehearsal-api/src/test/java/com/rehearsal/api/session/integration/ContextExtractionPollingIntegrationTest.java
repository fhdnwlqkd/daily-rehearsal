package com.rehearsal.api.session.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextCollectionState;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ContextExtractionPollingIntegrationTest {

  private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(5);

  @Autowired private CreateSessionUseCase createSessionUseCase;
  @Autowired private SubmitContextExtractionUseCase submitContextExtractionUseCase;
  @Autowired private GetContextExtractionUseCase getContextExtractionUseCase;

  @Test
  void briefingWithAllRequiredValuesCompletesThroughPolling() throws InterruptedException {
    ClientSession session = createSessionUseCase.createSession(SituationType.DATE);

    ClientSession submitted =
        submitContextExtractionUseCase.submitBriefingExtraction(
            session.getSessionId(), "I want to feel warm_natural at the first greeting.");

    assertThat(submitted.getContextStatus()).isEqualTo(ContextStatus.EXTRACTING);

    ContextCollectionState completed =
        awaitContextStatus(session.getSessionId(), ContextStatus.COMPLETED);

    assertThat(completed.context().values())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("critical_moment", "I want to feel warm_natural at the first greeting.");
    assertThat(completed.missingSlotKeys()).isEmpty();
    assertThat(completed.followUpQuestions()).isEmpty();
  }

  @Test
  void followUpMergesMissingRequiredValueThroughPolling() throws InterruptedException {
    ClientSession session = createSessionUseCase.createSession(SituationType.DATE);

    submitContextExtractionUseCase.submitBriefingExtraction(
        session.getSessionId(), "I am worried about the first greeting.");

    ContextCollectionState followUpRequired =
        awaitContextStatus(session.getSessionId(), ContextStatus.FOLLOW_UP_REQUIRED);
    assertThat(followUpRequired.missingSlotKeys()).containsExactly("desired_persona");
    assertThat(followUpRequired.followUpQuestions()).isNotEmpty();

    ClientSession submitted =
        submitContextExtractionUseCase.submitFollowUpExtraction(
            session.getSessionId(), "warm_natural");

    assertThat(submitted.getContextStatus()).isEqualTo(ContextStatus.MERGING);

    ContextCollectionState completed =
        awaitContextStatus(session.getSessionId(), ContextStatus.COMPLETED);
    assertThat(completed.context().values())
        .containsEntry("desired_persona", "warm_natural")
        .containsEntry("critical_moment", "I am worried about the first greeting.");
    assertThat(completed.missingSlotKeys()).isEmpty();
    assertThat(completed.followUpQuestions()).isEmpty();
  }

  private ContextCollectionState awaitContextStatus(String sessionId, ContextStatus expectedStatus)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(POLLING_TIMEOUT);
    ContextCollectionState state = getContextExtractionUseCase.getContext(sessionId);

    while (Instant.now().isBefore(deadline)) {
      if (state.status() == expectedStatus) {
        return state;
      }
      Thread.sleep(50);
      state = getContextExtractionUseCase.getContext(sessionId);
    }

    throw new AssertionError(
        "Expected context status " + expectedStatus + " but was " + state.status());
  }
}
