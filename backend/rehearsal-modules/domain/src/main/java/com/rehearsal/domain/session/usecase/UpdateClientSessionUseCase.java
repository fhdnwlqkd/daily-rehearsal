package com.rehearsal.domain.session.usecase;

import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.command.CompleteSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.UpdateBriefingTranscriptCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFeedbackResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFinalResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSelectedOutfitCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSimulationDraftCommand;

public interface UpdateClientSessionUseCase {

  ClientSession updateBriefingTranscript(UpdateBriefingTranscriptCommand command);

  ClientSession updateContext(UpdateSessionContextCommand command);

  ClientSession completeContext(CompleteSessionContextCommand command);

  ClientSession updateSelectedOutfit(UpdateSelectedOutfitCommand command);

  ClientSession updateSimulationDraft(UpdateSimulationDraftCommand command);

  ClientSession updateFeedbackResult(UpdateFeedbackResultCommand command);

  ClientSession updateFinalResult(UpdateFinalResultCommand command);
}
