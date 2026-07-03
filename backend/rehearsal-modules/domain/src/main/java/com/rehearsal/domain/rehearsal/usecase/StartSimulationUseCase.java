package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.SimulationStart;

public interface StartSimulationUseCase {

  SimulationStart startSimulation(String sessionId);
}
