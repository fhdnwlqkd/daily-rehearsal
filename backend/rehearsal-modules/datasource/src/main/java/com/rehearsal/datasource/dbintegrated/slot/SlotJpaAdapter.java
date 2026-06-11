package com.rehearsal.datasource.dbintegrated.slot;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotOptionJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaItemJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotOptionJpaRepository;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotSchemaItemJpaRepository;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotSchemaJpaRepository;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.repository.ContextSlotSchemaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotJpaAdapter implements ContextSlotSchemaRepository {

  private final ContextSlotSchemaJpaRepository schemaRepository;
  private final ContextSlotSchemaItemJpaRepository schemaItemRepository;
  private final ContextSlotOptionJpaRepository optionRepository;

  @Override
  public Optional<ContextSlotSchema> findActiveBySchemaKey(String schemaKey) {
    return schemaRepository.findBySchemaKeyAndActiveTrue(schemaKey).map(this::assembleSchema);
  }

  @Override
  public List<String> findActiveSchemaKeys() {
    return schemaRepository.findActiveSchemaKeys();
  }

  private ContextSlotSchema assembleSchema(ContextSlotSchemaJpaEntity schema) {
    List<ContextSlotSchemaItemJpaEntity> items = findActiveItems(schema);
    Map<Long, List<ContextSlotOptionJpaEntity>> optionsBySlotId = findOptionsBySlotId(items);
    return schema.toDomain(items, optionsBySlotId);
  }

  private List<ContextSlotSchemaItemJpaEntity> findActiveItems(ContextSlotSchemaJpaEntity schema) {
    return schemaItemRepository.findBySchema_IdAndActiveTrueOrderByPriorityAsc(schema.getId());
  }

  private Map<Long, List<ContextSlotOptionJpaEntity>> findOptionsBySlotId(
      List<ContextSlotSchemaItemJpaEntity> items) {
    List<Long> slotIds = items.stream().map(item -> item.getSlot().getId()).distinct().toList();

    if (slotIds.isEmpty()) {
      return Map.of();
    }

    return optionRepository.findBySlot_IdIn(slotIds).stream()
        .collect(Collectors.groupingBy(option -> option.getSlot().getId()));
  }
}
