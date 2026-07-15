package com.rehearsal.datasource.dbintegrated.session.repository;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RehearsalSessionJpaRepository
    extends JpaRepository<RehearsalSessionJpaEntity, String> {}
