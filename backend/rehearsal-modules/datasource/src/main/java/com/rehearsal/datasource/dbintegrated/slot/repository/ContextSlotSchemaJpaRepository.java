package com.rehearsal.datasource.dbintegrated.slot.repository;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContextSlotSchemaJpaRepository
    extends JpaRepository<ContextSlotSchemaJpaEntity, Long> {

  Optional<ContextSlotSchemaJpaEntity> findBySchemaKeyAndActiveTrue(String schemaKey);

  boolean existsBySchemaKey(String schemaKey);

  @Query(
      """
      select schema.schemaKey
      from ContextSlotSchemaJpaEntity schema
      where schema.active = true
      order by schema.schemaKey asc
      """)
  List<String> findActiveSchemaKeys();
}
