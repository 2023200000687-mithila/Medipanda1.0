package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document; import java.util.Date;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("notifications")
public class Notification { @Id private String id; private String userId, title, message, type, orderId; private boolean read; private Date createdAt; }
