package com.rehearsal.datasource.dbintegrated.slot.repository;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotOptionJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextSlotOptionJpaRepository
    extends JpaRepository<ContextSlotOptionJpaEntity, Long> {

  boolean existsBySlot_IdAndOptionKey(Long slotId, String optionKey);

  boolean existsBySlot_Id(Long slotId);

  @EntityGraph(attributePaths = {"slot"})
  List<ContextSlotOptionJpaEntity> findBySlot_IdIn(Collection<Long> slotIds);
}
