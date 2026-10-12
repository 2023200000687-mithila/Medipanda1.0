package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("pharmacies")
public class Pharmacy {
 @Id private String id; private String name, area, street, image, rating, deliveryTime, description, ownerUserId;
 private boolean open; private double latitude, longitude; private String locationAddress;
}
