package com.fujitsu.events.mapper;

import com.fujitsu.events.dto.EventRequest;
import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.model.Event;
import java.time.Instant;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventMapper {

    @Mapping(target = "timestamp", source = "effectiveTimestamp")
    @Mapping(target = "receivedAt", source = "receivedAt")
    Event toEntity(EventRequest request, LocalDateTime effectiveTimestamp, Instant receivedAt);

    EventResponse toResponse(Event event);
}
