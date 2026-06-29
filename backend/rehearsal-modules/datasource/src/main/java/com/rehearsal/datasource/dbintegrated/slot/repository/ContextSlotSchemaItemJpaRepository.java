package com.rehearsal.datasource.dbintegrated.slot.repository;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotSchemaItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextSlotSchemaItemJpaRepository
    extends JpaRepository<ContextSlotSchemaItemJpaEntity, Long> {

  boolean existsBySchema_IdAndSlot_Id(Long schemaId, Long slotId);

  boolean existsBySchema_Id(Long schemaId);

  boolean existsBySlot_Id(Long slotId);

  @EntityGraph(attributePaths = {"schema", "slot", "slot.defaultContextSlotOption"})
  List<ContextSlotSchemaItemJpaEntity> findBySchema_IdAndActiveTrueOrderByPriorityAsc(
      Long schemaId);

  @EntityGraph(attributePaths = {"schema", "slot", "slot.defaultContextSlotOption"})
  List<ContextSlotSchemaItemJpaEntity> findBySchema_IdOrderByPriorityAsc(Long schemaId);
}
