package com.medipanda.repository;
import com.medipanda.model.SiteSettings; import org.springframework.data.mongodb.repository.MongoRepository;
public interface SiteSettingsRepository extends MongoRepository<SiteSettings,String>{}
