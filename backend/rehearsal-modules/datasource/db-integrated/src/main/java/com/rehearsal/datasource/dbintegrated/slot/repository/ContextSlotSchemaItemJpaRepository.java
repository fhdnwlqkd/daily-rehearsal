package com.rehearsal.datasource.dbintegrated.slot.repository;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextSlotSchemaItemJpaRepository
    extends JpaRepository<ContextSlotSchemaItemJpaEntity, Long> {

  @EntityGraph(attributePaths = {"schema", "slot", "slot.defaultContextSlotOption"})
  List<ContextSlotSchemaItemJpaEntity> findBySchema_IdAndActiveTrueOrderByPriorityAsc(
      Long schemaId);
}
