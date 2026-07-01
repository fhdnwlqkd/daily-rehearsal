package com.rehearsal.datasource.dbintegrated.slot;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotOptionJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaItemJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaJpaEntity;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotJpaRepository;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotOptionJpaRepository;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotSchemaItemJpaRepository;
import com.rehearsal.datasource.dbintegrated.slot.repository.ContextSlotSchemaJpaRepository;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.repository.ContextSlotSchemaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("slot-admin")
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotJpaAdapter implements ContextSlotSchemaRepository {

  private final ContextSlotSchemaJpaRepository schemaRepository;
  private final ContextSlotJpaRepository slotRepository;
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

  @Override
  public boolean existsBySchemaKey(String schemaKey) {
    return schemaRepository.existsBySchemaKey(schemaKey);
  }

  @Override
  public boolean existsBySlotKey(String slotKey) {
    return slotRepository.existsBySlotKey(slotKey);
  }

  @Override
  public boolean existsOptionKey(Long slotId, String optionKey) {
    return optionRepository.existsBySlot_IdAndOptionKey(slotId, optionKey);
  }

  @Override
  public boolean existsSchemaItem(Long schemaId, Long slotId) {
    return schemaItemRepository.existsBySchema_IdAndSlot_Id(schemaId, slotId);
  }

  @Override
  public boolean existsSchemaItemBySchemaId(Long schemaId) {
    return schemaItemRepository.existsBySchema_Id(schemaId);
  }

  @Override
  public boolean existsSchemaItemBySlotId(Long slotId) {
    return schemaItemRepository.existsBySlot_Id(slotId);
  }

  @Override
  public boolean existsOptionBySlotId(Long slotId) {
    return optionRepository.existsBySlot_Id(slotId);
  }

  @Override
  public boolean existsSlotDefaultOption(Long optionId) {
    return slotRepository.existsByDefaultContextSlotOption_Id(optionId);
  }

  @Override
  public Optional<ContextSlot> findSlotById(Long slotId) {
    return slotRepository.findById(slotId).map(this::toSlotDomain);
  }

  @Override
  public Optional<ContextSlotOption> findOptionById(Long optionId) {
    return optionRepository.findById(optionId).map(ContextSlotOptionJpaEntity::toDomain);
  }

  @Override
  public List<ContextSlot> findAllSlots() {
    return slotRepository.findAllByOrderBySlotKeyAsc().stream().map(this::toSlotDomain).toList();
  }

  @Override
  @Transactional
  public ContextSlotSchema createSchema(ContextSlotSchema schema) {
    ContextSlotSchemaJpaEntity schemaEntity = ContextSlotSchemaJpaEntity.from(schema);
    return assembleSchemaIncludingInactive(schemaRepository.save(schemaEntity));
  }

  @Override
  @Transactional
  public ContextSlotSchema updateSchema(ContextSlotSchema schema) {
    ContextSlotSchemaJpaEntity schemaEntity = findSchemaEntity(schema.id());
    schemaEntity.update(schema.name(), schema.maxFollowUpAttempt(), schema.active());
    return assembleSchemaIncludingInactive(schemaEntity);
  }

  @Override
  @Transactional
  public ContextSlot createSlot(ContextSlot slot) {
    ContextSlotJpaEntity slotEntity = ContextSlotJpaEntity.from(slot);
    return toSlotDomain(slotRepository.save(slotEntity));
  }

  @Override
  @Transactional
  public ContextSlot updateSlot(ContextSlot slot) {
    ContextSlotJpaEntity slotEntity = findSlotEntity(slot.id());
    ContextSlotOptionJpaEntity defaultOption =
        slot.defaultOption() == null ? null : findOptionEntity(slot.defaultOption().id());
    slotEntity.update(
        slot.label(),
        slot.extractionHint(),
        slot.followUpHint(),
        slot.defaultLiteralValue(),
        defaultOption);
    return toSlotDomain(slotEntity);
  }

  @Override
  @Transactional
  public ContextSlot createOption(ContextSlot slot) {
    ContextSlotJpaEntity slotEntity = findSlotEntity(slot.id());
    ContextSlotOptionJpaEntity option =
        ContextSlotOptionJpaEntity.from(slotEntity, firstOption(slot));
    optionRepository.save(option);
    return toSlotDomain(slotEntity);
  }

  @Override
  @Transactional
  public ContextSlotOption updateOption(ContextSlotOption option) {
    ContextSlotOptionJpaEntity optionEntity = findOptionEntity(option.id());
    optionEntity.update(option.label());
    return optionEntity.toDomain();
  }

  @Override
  @Transactional
  public ContextSlotSchema createSchemaItem(ContextSlotSchema schema) {
    ContextSlotSchemaItem item = firstSchemaItem(schema);
    ContextSlotSchemaJpaEntity schemaEntity = findSchemaEntity(schema.id());
    ContextSlotJpaEntity slotEntity = findSlotEntity(item.slot().id());
    ContextSlotSchemaItemJpaEntity itemEntity =
        ContextSlotSchemaItemJpaEntity.from(schemaEntity, slotEntity, item);
    schemaItemRepository.save(itemEntity);
    return assembleSchemaIncludingInactive(schemaEntity);
  }

  @Override
  @Transactional
  public ContextSlotSchema updateSchemaItem(ContextSlotSchemaItem item) {
    ContextSlotSchemaItemJpaEntity itemEntity = findSchemaItemEntity(item.id());
    itemEntity.update(item.requiredLevel(), item.priority(), item.active());
    return assembleSchemaIncludingInactive(itemEntity.getSchema());
  }

  @Override
  @Transactional
  public void deleteSchema(ContextSlotSchema schema) {
    schemaRepository.delete(findSchemaEntity(schema.id()));
  }

  @Override
  @Transactional
  public void deleteSlot(ContextSlot slot) {
    slotRepository.delete(findSlotEntity(slot.id()));
  }

  @Override
  @Transactional
  public void deleteOption(ContextSlotOption option) {
    optionRepository.delete(findOptionEntity(option.id()));
  }

  @Override
  @Transactional
  public void deleteSchemaItem(ContextSlotSchemaItem item) {
    schemaItemRepository.delete(findSchemaItemEntity(item.id()));
  }

  private ContextSlotSchema assembleSchema(ContextSlotSchemaJpaEntity schema) {
    List<ContextSlotSchemaItemJpaEntity> items = findActiveItems(schema);
    Map<Long, List<ContextSlotOptionJpaEntity>> optionsBySlotId = findOptionsBySlotId(items);
    return schema.toDomain(items, optionsBySlotId);
  }

  private ContextSlotSchema assembleSchemaIncludingInactive(ContextSlotSchemaJpaEntity schema) {
    List<ContextSlotSchemaItemJpaEntity> items = findAllItems(schema);
    Map<Long, List<ContextSlotOptionJpaEntity>> optionsBySlotId = findOptionsBySlotId(items);
    return schema.toDomain(items, optionsBySlotId);
  }

  private ContextSlot toSlotDomain(ContextSlotJpaEntity slot) {
    List<ContextSlotOptionJpaEntity> options =
        optionRepository.findBySlot_IdIn(List.of(slot.getId()));
    return slot.toDomain(options);
  }

  private ContextSlotOption firstOption(ContextSlot slot) {
    return slot.options().get(0);
  }

  private ContextSlotSchemaItem firstSchemaItem(ContextSlotSchema schema) {
    return schema.items().get(0);
  }

  private List<ContextSlotSchemaItemJpaEntity> findActiveItems(ContextSlotSchemaJpaEntity schema) {
    return schemaItemRepository.findBySchema_IdAndActiveTrueOrderByPriorityAsc(schema.getId());
  }

  private List<ContextSlotSchemaItemJpaEntity> findAllItems(ContextSlotSchemaJpaEntity schema) {
    return schemaItemRepository.findBySchema_IdOrderByPriorityAsc(schema.getId());
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

  private ContextSlotSchemaJpaEntity findSchemaEntity(Long schemaId) {
    return schemaRepository
        .findById(schemaId)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND, "Context slot schema not found. schemaId=" + schemaId));
  }

  private ContextSlotJpaEntity findSlotEntity(Long slotId) {
    return slotRepository
        .findById(slotId)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND, "Context slot not found. slotId=" + slotId));
  }

  private ContextSlotOptionJpaEntity findOptionEntity(Long optionId) {
    return optionRepository
        .findById(optionId)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND, "Context slot option not found. optionId=" + optionId));
  }

  private ContextSlotSchemaItemJpaEntity findSchemaItemEntity(Long itemId) {
    return schemaItemRepository
        .findById(itemId)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND, "Context slot schema item not found. itemId=" + itemId));
  }
}
