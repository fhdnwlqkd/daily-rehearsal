package com.rehearsal.datasource.dbintegrated.slot.repository;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextSlotSchemaJpaRepository
    extends JpaRepository<ContextSlotSchemaJpaEntity, Long> {

  Optional<ContextSlotSchemaJpaEntity> findBySchemaKeyAndActiveTrue(String schemaKey);
}
