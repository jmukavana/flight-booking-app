package com.resilire.itinerary.infrastructure.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ItineraryRedisRepository extends CrudRepository<ItineraryDocument, String> {

    List<ItineraryDocument> findByTravellerId(String travellerId);
}
