package com.medipanda.repository;
import com.medipanda.model.Notification; import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface NotificationRepository extends MongoRepository<Notification,String> { List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(String userId); List<Notification> findByUserIdAndReadFalse(String userId); long countByUserIdAndReadFalse(String userId); }
