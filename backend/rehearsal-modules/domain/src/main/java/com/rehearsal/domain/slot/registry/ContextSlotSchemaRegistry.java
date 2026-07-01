package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ContextSlotSchemaRegistry {

  private static final int MAX_FOLLOW_UP_ATTEMPT = 1;
  private static final Map<SituationType, SlotSchemaDefinition> SCHEMAS =
      Map.of(
          SituationType.DATE, dateSchema(),
          SituationType.BUSINESS_MEETING, businessMeetingSchema());

  private ContextSlotSchemaRegistry() {}

  public static Optional<ContextSlotSchema> findBySituationType(SituationType situationType) {
    return Optional.ofNullable(SCHEMAS.get(situationType)).map(ContextSlotSchemaRegistry::toDomain);
  }

  public static Optional<ContextSlotSchema> findByKey(String schemaKey) {
    if (schemaKey == null || schemaKey.isBlank()) {
      return Optional.empty();
    }
    return SituationType.findByKey(schemaKey.strip())
        .flatMap(ContextSlotSchemaRegistry::findBySituationType);
  }

  public static List<String> schemaKeys() {
    return SCHEMAS.keySet().stream().map(SituationType::key).sorted().toList();
  }

  private static SlotSchemaDefinition dateSchema() {
    return new SlotSchemaDefinition(
        SituationType.DATE,
        "Date Context Slot Schema",
        MAX_FOLLOW_UP_ATTEMPT,
        List.of(
            item(desiredPersona(), RequiredLevel.REQUIRED, 10),
            item(criticalMoment(), RequiredLevel.REQUIRED, 20),
            item(outfitDirection(), RequiredLevel.OPTIONAL, 30)));
  }

  private static SlotSchemaDefinition businessMeetingSchema() {
    return new SlotSchemaDefinition(
        SituationType.BUSINESS_MEETING,
        "Business Meeting Context Slot Schema",
        MAX_FOLLOW_UP_ATTEMPT,
        List.of(
            item(desiredPersona(), RequiredLevel.REQUIRED, 10),
            item(criticalMoment(), RequiredLevel.REQUIRED, 20),
            item(outfitDirection(), RequiredLevel.OPTIONAL, 30)));
  }

  private static SlotSchemaItemDefinition item(
      SlotDefinition slot, RequiredLevel requiredLevel, int priority) {
    return new SlotSchemaItemDefinition(slot, requiredLevel, priority);
  }

  private static SlotDefinition desiredPersona() {
    return new SlotDefinition(
        "desired_persona",
        "Desired persona",
        SlotType.SINGLE_SELECT,
        "Normalize the persona the user wants to show tomorrow.",
        "What kind of impression do you want to leave tomorrow?",
        null,
        "calm_confident",
        List.of(
            option("calm_confident", "Calm confident"),
            option("warm_natural", "Warm natural"),
            option("sharp_prepared", "Sharp prepared")));
  }

  private static SlotDefinition criticalMoment() {
    return new SlotDefinition(
        "critical_moment",
        "Critical moment",
        SlotType.TEXT,
        "Extract the moment the user is most worried about or wants to rehearse.",
        "Which moment are you most worried about?",
        "first greeting and opening conversation",
        null,
        List.of());
  }

  private static SlotDefinition outfitDirection() {
    return new SlotDefinition(
        "outfit_direction",
        "Outfit direction",
        SlotType.SINGLE_SELECT,
        "Normalize the outfit mood the user wants to try.",
        "What outfit mood would help this situation?",
        null,
        "neat_casual",
        List.of(
            option("neat_casual", "Neat casual"),
            option("formal_clean", "Formal clean"),
            option("soft_friendly", "Soft friendly")));
  }

  private static SlotOptionDefinition option(String optionKey, String label) {
    return new SlotOptionDefinition(optionKey, label);
  }

  private static ContextSlotSchema toDomain(SlotSchemaDefinition definition) {
    return new ContextSlotSchema(
        null,
        definition.situationType().key(),
        definition.name(),
        definition.maxFollowUpAttempt(),
        true,
        definition.items().stream().map(ContextSlotSchemaRegistry::toDomain).toList());
  }

  private static ContextSlotSchemaItem toDomain(SlotSchemaItemDefinition definition) {
    return new ContextSlotSchemaItem(
        null, toDomain(definition.slot()), definition.requiredLevel(), definition.priority(), true);
  }

  private static ContextSlot toDomain(SlotDefinition definition) {
    List<ContextSlotOption> options =
        definition.options().stream().map(ContextSlotSchemaRegistry::toDomain).toList();
    return new ContextSlot(
        null,
        definition.slotKey(),
        definition.label(),
        definition.slotType(),
        definition.extractionHint(),
        definition.followUpHint(),
        definition.defaultLiteralValue(),
        defaultOption(definition, options),
        options);
  }

  private static ContextSlotOption defaultOption(
      SlotDefinition definition, List<ContextSlotOption> options) {
    if (definition.defaultOptionKey() == null || definition.defaultOptionKey().isBlank()) {
      return null;
    }
    return options.stream()
        .filter(option -> option.optionKey().equals(definition.defaultOptionKey()))
        .findFirst()
        .orElse(null);
  }

  private static ContextSlotOption toDomain(SlotOptionDefinition definition) {
    return new ContextSlotOption(null, definition.optionKey(), definition.label());
  }
}
