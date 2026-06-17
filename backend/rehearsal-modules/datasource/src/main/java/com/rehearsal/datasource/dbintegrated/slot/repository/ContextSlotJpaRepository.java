package com.rehearsal.datasource.dbintegrated.slot.repository;

import com.rehearsal.datasource.dbintegrated.slot.entity.ContextSlotJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextSlotJpaRepository extends JpaRepository<ContextSlotJpaEntity, Long> {

  boolean existsBySlotKey(String slotKey);

  boolean existsByDefaultContextSlotOption_Id(Long optionId);

  List<ContextSlotJpaEntity> findAllByOrderBySlotKeyAsc();
}
