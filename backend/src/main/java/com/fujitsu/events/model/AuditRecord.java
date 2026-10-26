package com.fujitsu.events.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "audit_records",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_audit_records_event_id", columnNames = "event_ref_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_ref_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_audit_records_event"))
    private Event event;

    @Column(name = "audited_at", nullable = false)
    private Instant auditedAt;

    public AuditRecord(Event event, Instant auditedAt) {
        this.event = event;
        this.auditedAt = auditedAt;
    }
}
