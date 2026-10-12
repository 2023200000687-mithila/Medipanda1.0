package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document; import java.util.Date;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("riders")
public class Rider { @Id private String id; private String userId; private String name, phone, vehicle, status; private double latitude, longitude; private double earnings; private Date locationUpdatedAt; private String locationAddress; }
