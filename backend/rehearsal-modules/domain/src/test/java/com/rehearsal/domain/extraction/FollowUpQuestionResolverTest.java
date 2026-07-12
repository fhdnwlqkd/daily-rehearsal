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

    String question = resolver.resolve(schema, List.of("desired_persona", "critical_moment"), 0);

    assertThat(question)
        .isEqualTo("내일의 장면이 거의 완성됐어요. 마지막으로 내일 어떤 인상을 남기고 싶나요? 가장 걱정되는 순간은 언제인가요?");
  }

  @Test
  void returnsNullWhenFollowUpAttemptReachedMax() {
    ContextSlotSchemaType schema = SlotExtractionTestFixtures.p1Schema();

    String question = resolver.resolve(schema, List.of("desired_persona"), 1);

    assertThat(question).isNull();
  }
}
