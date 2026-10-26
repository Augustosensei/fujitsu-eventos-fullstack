package com.fujitsu.events.repository;

import com.fujitsu.events.model.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    @Override
    @EntityGraph(attributePaths = "event")
    Page<AuditRecord> findAll(Pageable pageable);
}
