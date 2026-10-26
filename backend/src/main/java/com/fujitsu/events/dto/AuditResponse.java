package com.fujitsu.events.dto;

import java.time.Instant;

public record AuditResponse(Long id, String eventId, Instant auditedAt) {}
