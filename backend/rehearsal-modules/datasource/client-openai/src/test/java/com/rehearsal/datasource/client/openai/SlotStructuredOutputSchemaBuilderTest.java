package com.rehearsal.datasource.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.openai.core.JsonValue;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SlotStructuredOutputSchemaBuilderTest {

  private final SlotStructuredOutputSchemaBuilder builder = new SlotStructuredOutputSchemaBuilder();

  @Test
  void buildsStructuredOutputSchemaFromActiveSlots() {
    ContextSlotSchema schema = p1Schema();

    ResponseFormatTextJsonSchemaConfig responseFormat = builder.build(schema);
    Map<String, JsonValue> jsonSchema = responseFormat.schema()._additionalProperties();

    assertThat(responseFormat.name()).isEqualTo("context_slot_extraction");
    assertThat(responseFormat.strict()).hasValue(true);
    assertThat(responseFormat._type()).isEqualTo(JsonValue.from("json_schema"));
    assertThat(jsonSchema).isEqualTo(expectedRootSchema());
  }

  @Test
  void buildsChatCompletionResponseFormatWithSameSchema() {
    ContextSlotSchema schema = p1Schema();

    ResponseFormatJsonSchema responseFormat = builder.buildChatResponseFormat(schema);
    ResponseFormatJsonSchema.JsonSchema jsonSchema = responseFormat.jsonSchema();

    assertThat(jsonSchema.name()).isEqualTo("context_slot_extraction");
    assertThat(jsonSchema.strict()).hasValue(true);
    assertThat(responseFormat._type()).isEqualTo(JsonValue.from("json_schema"));
    assertThat(jsonSchema.schema()).isPresent();
    assertThat(jsonSchema.schema().orElseThrow()._additionalProperties())
        .isEqualTo(expectedRootSchema());
  }

  private Map<String, JsonValue> expectedRootSchema() {
    Map<String, JsonValue> slots = objectSchema();
    Map<String, JsonValue> slotProperties = new LinkedHashMap<>();
    slotProperties.put(
        "situation_type", json(singleSelectSchema("presentation", "date", "daily_reset")));
    slotProperties.put("desired_persona", json(singleSelectSchema("calm_confident")));
    slotProperties.put("critical_moment", json(nullableStringSchema()));
    slotProperties.put("anxiety_point", json(nullableStringSchema()));
    slotProperties.put("place_context", json(nullableStringSchema()));
    slots.put("properties", json(slotProperties));
    slots.put(
        "required",
        json(
            List.of(
                "situation_type",
                "desired_persona",
                "critical_moment",
                "anxiety_point",
                "place_context")));

    Map<String, JsonValue> rootProperties = new LinkedHashMap<>();
    rootProperties.put("slots", json(slots));

    Map<String, JsonValue> root = objectSchema();
    root.put("properties", json(rootProperties));
    root.put("required", json(List.of("slots")));
    return root;
  }

  private Map<String, JsonValue> objectSchema() {
    Map<String, JsonValue> schema = new LinkedHashMap<>();
    schema.put("type", json("object"));
    schema.put("additionalProperties", json(false));
    return schema;
  }

  private Map<String, JsonValue> singleSelectSchema(String... optionKeys) {
    Map<String, JsonValue> schema = nullableStringSchema();
    List<String> enumValues = new ArrayList<>(List.of(optionKeys));
    enumValues.add(null);
    schema.put("enum", json(enumValues));
    return schema;
  }

  private Map<String, JsonValue> nullableStringSchema() {
    Map<String, JsonValue> schema = new LinkedHashMap<>();
    schema.put("type", json(List.of("string", "null")));
    return schema;
  }

  private JsonValue json(Object value) {
    return JsonValue.from(value);
  }

  private ContextSlotSchema p1Schema() {
    ContextSlot situationType = getContextSlot();

    ContextSlotOption calmConfident = new ContextSlotOption(4L, "calm_confident", "차분하고 신뢰감 있는 모습");
    ContextSlot desiredPersona =
        new ContextSlot(
            2L,
            "desired_persona",
            "되고 싶은 모습",
            SlotType.SINGLE_SELECT,
            "보여주고 싶은 태도나 인상을 추출한다.",
            "내일 어떤 모습으로 보이고 싶은지 알려주세요.",
            null,
            calmConfident,
            List.of(calmConfident));

    ContextSlot criticalMoment =
        new ContextSlot(
            3L,
            "critical_moment",
            "결정적 순간",
            SlotType.TEXT,
            "가장 리허설하고 싶은 순간을 추출한다.",
            "가장 걱정되는 순간은 언제인가요?",
            "첫 반응을 말해야 하는 순간",
            null,
            List.of());

    ContextSlot anxietyPoint =
        new ContextSlot(
            4L,
            "anxiety_point",
            "걱정 포인트",
            SlotType.TEXT,
            "걱정하거나 불편한 지점을 추출한다.",
            "내일 가장 신경 쓰이는 점이 있나요?",
            "처음 시작이 어색할 수 있음",
            null,
            List.of());

    ContextSlot placeContext =
        new ContextSlot(
            5L,
            "place_context",
            "장소 맥락",
            SlotType.TEXT,
            "내일 가게 될 장소나 공간 분위기를 추출한다.",
            "어디에서 일어나는 상황인지 알려주세요.",
            null,
            null,
            List.of());

    return new ContextSlotSchema(
        1L,
        "p1_offline_default",
        "P1 Offline Default Context Slot Schema",
        1,
        true,
        List.of(
            new ContextSlotSchemaItem(1L, situationType, RequiredLevel.REQUIRED, 10, true),
            new ContextSlotSchemaItem(2L, desiredPersona, RequiredLevel.REQUIRED, 20, true),
            new ContextSlotSchemaItem(3L, criticalMoment, RequiredLevel.REQUIRED, 30, true),
            new ContextSlotSchemaItem(4L, anxietyPoint, RequiredLevel.SOFT_REQUIRED, 40, true),
            new ContextSlotSchemaItem(5L, placeContext, RequiredLevel.OPTIONAL, 50, true)));
  }

  private static ContextSlot getContextSlot() {
    ContextSlotOption presentation = new ContextSlotOption(1L, "presentation", "발표");
    ContextSlotOption date = new ContextSlotOption(2L, "date", "소개팅/첫 만남");
    ContextSlotOption dailyReset = new ContextSlotOption(3L, "daily_reset", "일상 정돈");
    return new ContextSlot(
        1L,
        "situation_type",
        "상황 유형",
        SlotType.SINGLE_SELECT,
        "내일 상황을 분류한다.",
        "내일 어떤 상황인지 알려주세요.",
        null,
        dailyReset,
        List.of(presentation, date, dailyReset));
  }
}
