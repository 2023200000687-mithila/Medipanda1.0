package com.medipanda.repository;
import com.medipanda.model.Review; import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface ReviewRepository extends MongoRepository<Review,String> { List<Review> findByPharmacyIdOrderByCreatedAtDesc(String pharmacyId); Optional<Review> findByOrderIdAndCustomerId(String orderId,String customerId); }
