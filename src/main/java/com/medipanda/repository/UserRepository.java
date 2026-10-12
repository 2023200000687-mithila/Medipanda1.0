package com.medipanda.repository;
import com.medipanda.model.User; import org.springframework.data.mongodb.repository.MongoRepository; import java.util.Optional;
public interface UserRepository extends MongoRepository<User,String> {
 Optional<User> findByEmailIgnoreCase(String email);
 Optional<User> findByPhone(String phone);
 Optional<User> findByEmailIgnoreCaseOrPhone(String email,String phone);
}
