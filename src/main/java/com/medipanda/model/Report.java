package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document; import java.util.Date;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("reports")
public class Report { @Id private String id; private String userId; private String userName; private String role; private String subject; private String message; private String status; private String adminReply; private Date createdAt; private Date updatedAt; }
