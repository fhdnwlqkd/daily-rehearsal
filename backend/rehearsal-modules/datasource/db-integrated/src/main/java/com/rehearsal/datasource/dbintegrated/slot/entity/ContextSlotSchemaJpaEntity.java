package com.rehearsal.datasource.dbintegrated.slot.entity;

import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    name = "context_slot_schema",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_context_slot_schema_schema_key", columnNames = "schema_key"))
public class ContextSlotSchemaJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "schema_key", nullable = false, length = 64)
  private String schemaKey;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "max_follow_up_attempt", nullable = false)
  private int maxFollowUpAttempt;

  @Column(name = "active", nullable = false)
  private boolean active;

  public ContextSlotSchema toDomain(
      List<ContextSlotSchemaItemJpaEntity> itemEntities,
      Map<Long, List<ContextSlotOptionJpaEntity>> optionsBySlotId) {
    List<ContextSlotSchemaItem> items =
        itemEntities.stream().map(item -> item.toDomain(optionsBySlotId)).toList();

    return new ContextSlotSchema(id, schemaKey, name, maxFollowUpAttempt, active, items);
  }
}
