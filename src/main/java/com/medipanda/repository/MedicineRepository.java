package com.medipanda.repository;

import com.medipanda.model.Medicine;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MedicineRepository extends MongoRepository<Medicine,String> {
    List<Medicine> findByCategoryIgnoreCase(String category);
    List<Medicine> findByNameContainingIgnoreCase(String name);
    List<Medicine> findByPharmacyId(String pharmacyId);
}
