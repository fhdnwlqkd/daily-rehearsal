package com.rehearsal.api.rehearsal.controller;

import static com.rehearsal.api.support.SimulationTestFixtures.completedTurn;
import static com.rehearsal.api.support.SimulationTestFixtures.pendingTurn;
import static com.rehearsal.api.support.SimulationTestFixtures.plan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.api.support.RestDocsEnumValues;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationOutcome;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;
import com.rehearsal.domain.rehearsal.usecase.FinishSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs
@WebMvcTest(controllers = SimulationController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class SimulationControllerDocsTest {

  private static final String API_KEY = "test-api-key";
  private static final String SESSION_ID = "session-id";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StartSimulationUseCase startSimulationUseCase;
  @MockitoBean private FinishSimulationUseCase finishSimulationUseCase;
  @MockitoBean private SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase;
  @MockitoBean private GetTurnEvaluationUseCase getTurnEvaluationUseCase;
  @MockitoBean private SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase;
  @MockitoBean private GetNextOpponentLineUseCase getNextOpponentLineUseCase;
  @MockitoBean private SessionReader sessionReader;

  @Test
  void startSimulation() throws Exception {
    given(startSimulationUseCase.startSimulation(SESSION_ID))
        .willReturn(
            new SimulationStart(
                SESSION_ID, 1, 3, TurnGenerationMode.STATIC, plan("Hello, nice to meet you.")));

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/simulation/start", SESSION_ID)
                .header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currentTurn").value(1))
        .andExpect(jsonPath("$.data.opponentLine").isNotEmpty())
        .andDo(
            document(
                "simulation-start",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.currentTurn").description("Current simulation turn number"),
                    fieldWithPath("data.maxTurn").description("Maximum turn count"),
                    fieldWithPath("data.generationMode").description("Turn generation mode"),
                    fieldWithPath("data.sceneCue").description("Scene shown before the line"),
                    fieldWithPath("data.opponentLine").description("First opponent line"),
                    fieldWithPath("data.actionPrompt").description("Action requested from user"))));
  }

  @Test
  void submitTurnEvaluation() throws Exception {
    SimulationTurnAttempt pending = SimulationTurnAttempt.pending(1L, 1, "Nice to meet you too.");
    given(submitTurnEvaluationUseCase.submit(anyString(), anyInt(), anyString(), any()))
        .willReturn(pending);

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation", SESSION_ID, 1)
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"Nice to meet you too.\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.attemptNo").value(1))
        .andExpect(jsonPath("$.data.turnCompleted").value(false))
        .andDo(
            document(
                "simulation-submit-evaluation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(
                    parameterWithName("sessionId").description("Session ID"),
                    parameterWithName("turnNo").description("Simulation turn number")),
                requestFields(fieldWithPath("transcript").description("User STT transcript")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.turnNo").description("Simulation turn number"),
                    fieldWithPath("data.attemptNo").description("Evaluation attempt number"),
                    fieldWithPath("data.status")
                        .description(
                            "Evaluation status. Values: "
                                + RestDocsEnumValues.names(EvaluationStatus.class)),
                    fieldWithPath("data.turnCompleted")
                        .description("Always false while the evaluation is pending"))));
  }

  @Test
  void pollTurnEvaluation() throws Exception {
    SimulationTurnAttempt completed = SimulationTurnAttempt.pending(1L, 1, "Nice to meet you too.");
    completed.complete(
        new TurnEvaluationResult(
            TurnEvaluationOutcome.ACCEPTED, "Clear and natural response.", false));
    given(getTurnEvaluationUseCase.get(SESSION_ID, 1)).willReturn(completed);
    ClientSession session = org.mockito.Mockito.mock(ClientSession.class);
    given(session.getCurrentTurn()).willReturn(2);
    given(sessionReader.get(SESSION_ID)).willReturn(session);

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation", SESSION_ID, 1)
                .header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.outcome").value("ACCEPTED"))
        .andExpect(jsonPath("$.data.turnCompleted").value(true))
        .andExpect(jsonPath("$.data.feedback").isNotEmpty())
        .andDo(
            document(
                "simulation-poll-evaluation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(
                    parameterWithName("sessionId").description("Session ID"),
                    parameterWithName("turnNo").description("Simulation turn number")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.turnNo").description("Simulation turn number"),
                    fieldWithPath("data.attemptNo").description("Evaluation attempt number"),
                    fieldWithPath("data.status")
                        .description(
                            "Evaluation status. Values: "
                                + RestDocsEnumValues.names(EvaluationStatus.class)),
                    fieldWithPath("data.outcome")
                        .description("Turn outcome: ACCEPTED, RETRY_REQUIRED, or FORCED_ADVANCE"),
                    fieldWithPath("data.feedback").description("Evaluation feedback"),
                    fieldWithPath("data.fallback")
                        .description("Whether fallback feedback was used"),
                    fieldWithPath("data.turnCompleted")
                        .description(
                            "Whether this turn is complete and the client should proceed, independent of success"),
                    fieldWithPath("data.failureReason")
                        .type(JsonFieldType.STRING)
                        .optional()
                        .description("Failure reason when failed"))));
  }

  @Test
  void submitNextOpponentLine() throws Exception {
    SimulationTurn pending = pendingTurn(SESSION_ID, 2);
    given(submitNextOpponentLineUseCase.submitNextLine(SESSION_ID, 2)).willReturn(pending);

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/next-line", SESSION_ID, 2)
                .header("X-API-KEY", API_KEY))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andDo(
            document(
                "simulation-submit-next-line",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(
                    parameterWithName("sessionId").description("Session ID"),
                    parameterWithName("turnNo").description("Next simulation turn number")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.turnNo").description("Simulation turn number"),
                    fieldWithPath("data.status")
                        .description(
                            "Opponent line status. Values: "
                                + RestDocsEnumValues.names(OpponentLineStatus.class)))));
  }

  @Test
  void pollNextOpponentLine() throws Exception {
    SimulationTurn completed = completedTurn(SESSION_ID, 2, "What do you enjoy doing?");
    given(getNextOpponentLineUseCase.getNextLine(SESSION_ID, 2)).willReturn(completed);

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/next-line", SESSION_ID, 2)
                .header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.opponentLine").isNotEmpty())
        .andDo(
            document(
                "simulation-poll-next-line",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(
                    parameterWithName("sessionId").description("Session ID"),
                    parameterWithName("turnNo").description("Simulation turn number")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.turnNo").description("Simulation turn number"),
                    fieldWithPath("data.status")
                        .description(
                            "Opponent line status. Values: "
                                + RestDocsEnumValues.names(OpponentLineStatus.class)),
                    fieldWithPath("data.generationMode").description("Turn generation mode"),
                    fieldWithPath("data.sceneCue").description("Scene shown before the line"),
                    fieldWithPath("data.opponentLine").description("Generated opponent line"),
                    fieldWithPath("data.actionPrompt").description("Action requested from user"),
                    fieldWithPath("data.failureReason")
                        .type(JsonFieldType.STRING)
                        .optional()
                        .description("Failure reason when failed"))));
  }
}
