package com.rehearsal.domain.slot.repository;

import com.rehearsal.domain.slot.model.ContextSlotSchema;
import java.util.List;
import java.util.Optional;

public interface ContextSlotSchemaRepository {

  Optional<ContextSlotSchema> findActiveBySchemaKey(String schemaKey);

  List<String> findActiveSchemaKeys();
}
