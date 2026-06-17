package com.rehearsal.datasource.dbintegrated.slot.entity;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.SlotType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "context_slot",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_context_slot_slot_key", columnNames = "slot_key"))
public class ContextSlotJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "slot_key", nullable = false, length = 64)
  private String slotKey;

  @Column(name = "label", nullable = false, length = 100)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(name = "slot_type", nullable = false, length = 30)
  private SlotType slotType;

  @Column(name = "extraction_hint", length = 1000)
  private String extractionHint;

  @Column(name = "follow_up_hint", length = 300)
  private String followUpHint;

  @Column(name = "default_literal_value", length = 500)
  private String defaultLiteralValue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "default_context_slot_option_id")
  private ContextSlotOptionJpaEntity defaultContextSlotOption;

  public static ContextSlotJpaEntity create(
      String slotKey,
      String label,
      SlotType slotType,
      String extractionHint,
      String followUpHint,
      String defaultLiteralValue) {
    ContextSlotJpaEntity entity = new ContextSlotJpaEntity();
    entity.slotKey = slotKey;
    entity.label = label;
    entity.slotType = slotType;
    entity.extractionHint = extractionHint;
    entity.followUpHint = followUpHint;
    entity.defaultLiteralValue = defaultLiteralValue;
    return entity;
  }

  public static ContextSlotJpaEntity from(ContextSlot slot) {
    return create(
        slot.slotKey(),
        slot.label(),
        slot.slotType(),
        slot.extractionHint(),
        slot.followUpHint(),
        slot.defaultLiteralValue());
  }

  public void update(
      String label,
      String extractionHint,
      String followUpHint,
      String defaultLiteralValue,
      ContextSlotOptionJpaEntity defaultContextSlotOption) {
    this.label = label;
    this.extractionHint = extractionHint;
    this.followUpHint = followUpHint;
    this.defaultLiteralValue = defaultLiteralValue;
    this.defaultContextSlotOption = defaultContextSlotOption;
  }

  public ContextSlot toDomain(List<ContextSlotOptionJpaEntity> optionEntities) {
    List<ContextSlotOption> options =
        optionEntities == null
            ? List.of()
            : optionEntities.stream().map(ContextSlotOptionJpaEntity::toDomain).toList();

    return new ContextSlot(
        id,
        slotKey,
        label,
        slotType,
        extractionHint,
        followUpHint,
        defaultLiteralValue,
        defaultContextSlotOption == null ? null : defaultContextSlotOption.toDomain(),
        options);
  }
}
