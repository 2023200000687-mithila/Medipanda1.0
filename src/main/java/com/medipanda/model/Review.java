package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document; import java.util.Date;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("reviews")
public class Review { @Id private String id; private String orderId, customerId, customerName, pharmacyId, riderId; private int pharmacyRating, riderRating; private String comment; private Date createdAt; }
