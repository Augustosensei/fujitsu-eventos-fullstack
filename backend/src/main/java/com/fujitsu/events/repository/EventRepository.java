package com.fujitsu.events.repository;

import com.fujitsu.events.model.Event;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByEventId(String eventId);

    Optional<Event> findByEventId(String eventId);
}
