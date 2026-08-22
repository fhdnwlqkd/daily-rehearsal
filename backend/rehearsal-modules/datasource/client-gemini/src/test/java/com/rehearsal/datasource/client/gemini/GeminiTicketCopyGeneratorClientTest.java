package com.rehearsal.datasource.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.ticket.model.TicketGenerationCommand;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiTicketCopyGeneratorClientTest {

  private static final TicketGenerationCommand COMMAND =
      new TicketGenerationCommand(
          SituationType.DATE, Map.of(), "date-neat-casual", List.of(), List.of());

  @Test
  void acceptsConciseSingleLineTicketCopy() {
    GeminiTicketCopyGeneratorClient client =
        clientReturning(response("천천히 인사하기", "상대의 말을 끝까지 듣기", "막히면 가벼운 질문을 건네기"));

    var result = client.generate(COMMAND);

    assertThat(result.changeCard().todayAction()).isEqualTo("천천히 인사하기");
    assertThat(result.changeCard().tomorrowAttitude()).isEqualTo("상대의 말을 끝까지 듣기");
    assertThat(result.changeCard().ifThenPlan()).isEqualTo("막히면 가벼운 질문을 건네기");
  }

  @Test
  void rejectsCopyThatExceedsDisplayLengthLimit() {
    String tooLong = "가".repeat(GeminiTicketCopyGeneratorClient.TODAY_ACTION_MAX_LENGTH + 1);
    GeminiTicketCopyGeneratorClient client =
        clientReturning(response(tooLong, "차분함 유지하기", "막히면 잠시 호흡하기"));

    assertThatThrownBy(() -> client.generate(COMMAND))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("todayAction length limit");
  }

  @Test
  void rejectsCopyWithLineBreaks() {
    GeminiTicketCopyGeneratorClient client =
        clientReturning(response("천천히 인사하고\n상대를 바라보기", "차분함 유지하기", "막히면 잠시 호흡하기"));

    assertThatThrownBy(() -> client.generate(COMMAND))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("line break in todayAction");
  }

  private GeminiTicketCopyGeneratorClient clientReturning(String response) {
    return new GeminiTicketCopyGeneratorClient((model, userMessage, config) -> response);
  }

  private String response(String todayAction, String tomorrowAttitude, String ifThenPlan) {
    return """
        {
          "changeCard": {
            "todayAction": "%s",
            "tomorrowAttitude": "%s",
            "ifThenPlan": "%s"
          }
        }
        """
        .formatted(escapeJson(todayAction), escapeJson(tomorrowAttitude), escapeJson(ifThenPlan));
  }

  private String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }
}
