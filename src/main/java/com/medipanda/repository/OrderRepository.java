package com.medipanda.repository;

import com.medipanda.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String email);
    List<Order> findByPharmacyIdOrderByCreatedAtDesc(String pharmacyId);
    List<Order> findByRiderIdOrderByCreatedAtDesc(String riderId);
}
