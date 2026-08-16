package com.rehearsal.domain.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.extraction.service.FollowUpQuestionResolver;
import com.rehearsal.domain.slot.registry.ContextSlotSchemaType;
import java.util.List;
import org.junit.jupiter.api.Test;

class FollowUpQuestionResolverTest {

  private final FollowUpQuestionResolver resolver = new FollowUpQuestionResolver();

  @Test
  void buildsQuestionFromMissingSlotHints() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    String question =
        resolver.resolve(
            schema,
            List.of(
                "situation_detail", "desired_persona", "desired_outcome", "conversation_material"),
            0);

    assertThat(question)
        .isEqualTo(
            "내일의 장면이 거의 완성됐어요. 마지막으로 어떤 상황을 연습하는지 한마디로 알려주세요. "
                + "상대에게 어떤 인상을 남기고 싶나요? 이 상황이 어떻게 끝나면 좋을까요? "
                + "편하게 이야기하거나 활용할 수 있는 소재 하나를 알려주세요.");
  }

  @Test
  void returnsNullWhenFollowUpAttemptReachedMax() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    String question = resolver.resolve(schema, List.of("desired_persona"), 1);

    assertThat(question).isNull();
  }
}
