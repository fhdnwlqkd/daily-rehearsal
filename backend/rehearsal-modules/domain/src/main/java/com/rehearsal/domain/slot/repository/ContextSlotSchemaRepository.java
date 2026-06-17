package com.rehearsal.domain.slot.repository;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import java.util.List;
import java.util.Optional;

public interface ContextSlotSchemaRepository {

  Optional<ContextSlotSchema> findActiveBySchemaKey(String schemaKey);

  List<String> findActiveSchemaKeys();

  boolean existsBySchemaKey(String schemaKey);

  boolean existsBySlotKey(String slotKey);

  boolean existsOptionKey(Long slotId, String optionKey);

  boolean existsSchemaItem(Long schemaId, Long slotId);

  boolean existsSchemaItemBySchemaId(Long schemaId);

  boolean existsSchemaItemBySlotId(Long slotId);

  boolean existsOptionBySlotId(Long slotId);

  boolean existsSlotDefaultOption(Long optionId);

  Optional<ContextSlot> findSlotById(Long slotId);

  Optional<ContextSlotOption> findOptionById(Long optionId);

  List<ContextSlot> findAllSlots();

  ContextSlotSchema createSchema(ContextSlotSchema schema);

  ContextSlotSchema updateSchema(ContextSlotSchema schema);

  ContextSlot createSlot(ContextSlot slot);

  ContextSlot updateSlot(ContextSlot slot);

  ContextSlot createOption(ContextSlot slot);

  ContextSlotOption updateOption(ContextSlotOption option);

  ContextSlotSchema createSchemaItem(ContextSlotSchema schema);

  ContextSlotSchema updateSchemaItem(ContextSlotSchemaItem item);

  void deleteSchema(ContextSlotSchema schema);

  void deleteSlot(ContextSlot slot);

  void deleteOption(ContextSlotOption option);

  void deleteSchemaItem(ContextSlotSchemaItem item);
}
