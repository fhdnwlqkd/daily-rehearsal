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
  private static final Map<SituationType, ContextSlotSchema> SCHEMAS =
      Map.of(
          SituationType.DATE, dateSchema(),
          SituationType.BUSINESS_MEETING, businessMeetingSchema());

  private ContextSlotSchemaRegistry() {}

  public static Optional<ContextSlotSchema> findBySituationType(SituationType situationType) {
    return Optional.ofNullable(SCHEMAS.get(situationType));
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

  private static ContextSlotSchema dateSchema() {
    return new ContextSlotSchema(
        1L,
        SituationType.DATE.key(),
        "Date Context Slot Schema",
        MAX_FOLLOW_UP_ATTEMPT,
        true,
        List.of(
            item(1L, desiredPersona(1L), RequiredLevel.REQUIRED, 10),
            item(2L, criticalMoment(2L), RequiredLevel.REQUIRED, 20),
            item(3L, outfitDirection(3L), RequiredLevel.OPTIONAL, 30)));
  }

  private static ContextSlotSchema businessMeetingSchema() {
    return new ContextSlotSchema(
        2L,
        SituationType.BUSINESS_MEETING.key(),
        "Business Meeting Context Slot Schema",
        MAX_FOLLOW_UP_ATTEMPT,
        true,
        List.of(
            item(4L, desiredPersona(4L), RequiredLevel.REQUIRED, 10),
            item(5L, criticalMoment(5L), RequiredLevel.REQUIRED, 20),
            item(6L, outfitDirection(6L), RequiredLevel.OPTIONAL, 30)));
  }

  private static ContextSlotSchemaItem item(
      Long id, ContextSlot slot, RequiredLevel requiredLevel, int priority) {
    return new ContextSlotSchemaItem(id, slot, requiredLevel, priority, true);
  }

  private static ContextSlot desiredPersona(Long id) {
    ContextSlotOption calmConfident = option(id * 10 + 1, "calm_confident", "Calm confident");
    ContextSlotOption warmNatural = option(id * 10 + 2, "warm_natural", "Warm natural");
    ContextSlotOption sharpPrepared = option(id * 10 + 3, "sharp_prepared", "Sharp prepared");
    return new ContextSlot(
        id,
        "desired_persona",
        "Desired persona",
        SlotType.SINGLE_SELECT,
        "Normalize the persona the user wants to show tomorrow.",
        "What kind of impression do you want to leave tomorrow?",
        null,
        calmConfident,
        List.of(calmConfident, warmNatural, sharpPrepared));
  }

  private static ContextSlot criticalMoment(Long id) {
    return new ContextSlot(
        id,
        "critical_moment",
        "Critical moment",
        SlotType.TEXT,
        "Extract the moment the user is most worried about or wants to rehearse.",
        "Which moment are you most worried about?",
        "first greeting and opening conversation",
        null,
        List.of());
  }

  private static ContextSlot outfitDirection(Long id) {
    ContextSlotOption neatCasual = option(id * 10 + 1, "neat_casual", "Neat casual");
    ContextSlotOption formalClean = option(id * 10 + 2, "formal_clean", "Formal clean");
    ContextSlotOption softFriendly = option(id * 10 + 3, "soft_friendly", "Soft friendly");
    return new ContextSlot(
        id,
        "outfit_direction",
        "Outfit direction",
        SlotType.SINGLE_SELECT,
        "Normalize the outfit mood the user wants to try.",
        "What outfit mood would help this situation?",
        null,
        neatCasual,
        List.of(neatCasual, formalClean, softFriendly));
  }

  private static ContextSlotOption option(Long id, String optionKey, String label) {
    return new ContextSlotOption(id, optionKey, label);
  }
}
