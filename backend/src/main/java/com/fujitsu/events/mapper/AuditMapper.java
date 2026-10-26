package com.fujitsu.events.mapper;

import com.fujitsu.events.dto.AuditResponse;
import com.fujitsu.events.model.AuditRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditMapper {

    @Mapping(target = "eventId", source = "event.eventId")
    AuditResponse toResponse(AuditRecord auditRecord);
}
