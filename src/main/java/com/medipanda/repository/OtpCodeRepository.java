package com.medipanda.repository;
import com.medipanda.model.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface OtpCodeRepository extends MongoRepository<OtpCode,String> {
    Optional<OtpCode> findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email,String purpose);
}
