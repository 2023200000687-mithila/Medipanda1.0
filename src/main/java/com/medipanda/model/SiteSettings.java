package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("site_settings")
public class SiteSettings { @Id private String id; private String siteName,tagline,heroTitle,heroText,heroVideoUrl,logoUrl,contactEmail,contactPhone,footerText; private boolean maintenanceMode; }
