package com.fujitsu.events.service;

import com.fujitsu.events.dto.AuditResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.dto.PageResponse;
import com.fujitsu.events.mapper.AuditMapper;
import com.fujitsu.events.model.AuditRecord;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.repository.AuditRecordRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "auditedAt", "id");

    private final AuditRecordRepository auditRecordRepository;
    private final AuditMapper auditMapper;
    private final Paginator paginator;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void audit(Event event) {
        var auditRecord = auditRecordRepository.saveAndFlush(new AuditRecord(event, clock.instant()));
        log.info("Audit record created: id={}, eventId={}", auditRecord.getId(), event.getEventId());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> findPage(PageQuery query) {
        var page = paginator.fetch(
                query, NEWEST_FIRST, auditRecordRepository::findAll, auditRecordRepository::count);
        return PageResponse.from(page.map(auditMapper::toResponse));
    }
}
