package com.rehearsal.datasource.dbintegrated.slot.entity;

import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
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
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "context_slot_schema_item",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_context_slot_schema_item_schema_slot",
            columnNames = {"context_slot_schema_id", "context_slot_id"}))
public class ContextSlotSchemaItemJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "context_slot_schema_id", nullable = false)
  private ContextSlotSchemaJpaEntity schema;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "context_slot_id", nullable = false)
  private ContextSlotJpaEntity slot;

  @Enumerated(EnumType.STRING)
  @Column(name = "required_level", nullable = false, length = 30)
  private RequiredLevel requiredLevel;

  @Column(name = "priority", nullable = false)
  private int priority;

  @Column(name = "active", nullable = false)
  private boolean active;

  public static ContextSlotSchemaItemJpaEntity create(
      ContextSlotSchemaJpaEntity schema,
      ContextSlotJpaEntity slot,
      RequiredLevel requiredLevel,
      int priority,
      boolean active) {
    ContextSlotSchemaItemJpaEntity entity = new ContextSlotSchemaItemJpaEntity();
    entity.schema = schema;
    entity.slot = slot;
    entity.requiredLevel = requiredLevel;
    entity.priority = priority;
    entity.active = active;
    return entity;
  }

  public static ContextSlotSchemaItemJpaEntity from(
      ContextSlotSchemaJpaEntity schema, ContextSlotJpaEntity slot, ContextSlotSchemaItem item) {
    return create(schema, slot, item.requiredLevel(), item.priority(), item.active());
  }

  public void update(RequiredLevel requiredLevel, int priority, boolean active) {
    this.requiredLevel = requiredLevel;
    this.priority = priority;
    this.active = active;
  }

  public ContextSlotSchemaItem toDomain(
      Map<Long, List<ContextSlotOptionJpaEntity>> optionsBySlotId) {
    return new ContextSlotSchemaItem(
        id,
        slot.toDomain(optionsBySlotId.getOrDefault(slot.getId(), List.of())),
        requiredLevel,
        priority,
        active);
  }
}
