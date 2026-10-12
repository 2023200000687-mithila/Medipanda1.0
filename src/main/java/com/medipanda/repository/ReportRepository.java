package com.medipanda.repository;
import com.medipanda.model.Report; import org.springframework.data.mongodb.repository.MongoRepository; import java.util.*;
public interface ReportRepository extends MongoRepository<Report,String>{ List<Report> findByUserIdOrderByCreatedAtDesc(String userId); List<Report> findAllByOrderByCreatedAtDesc(); }
