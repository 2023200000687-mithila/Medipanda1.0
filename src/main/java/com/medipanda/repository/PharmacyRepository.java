package com.medipanda.repository;

import com.medipanda.model.Pharmacy;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PharmacyRepository extends MongoRepository<Pharmacy, String> {
    Optional<Pharmacy> findByOwnerUserId(String ownerUserId);
}
