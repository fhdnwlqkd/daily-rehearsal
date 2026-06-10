package com.rehearsal.datasource.dbintegrated.slot.entity;

import com.rehearsal.domain.slot.model.ContextSlotOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "context_slot_option",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_context_slot_option_slot_option_key",
            columnNames = {"context_slot_id", "option_key"}))
public class ContextSlotOptionJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "context_slot_id", nullable = false)
  private ContextSlotJpaEntity slot;

  @Column(name = "option_key", nullable = false, length = 64)
  private String optionKey;

  @Column(name = "label", nullable = false, length = 100)
  private String label;

  public ContextSlotOption toDomain() {
    return new ContextSlotOption(id, optionKey, label);
  }
}
