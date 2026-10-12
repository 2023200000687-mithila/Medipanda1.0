package com.medipanda.model;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Document("medicines")
public class Medicine { @Id private String id; private String name, genericName, category, pharmacyId, image, manufacturer; private double price, oldPrice; private int stock; private boolean prescriptionRequired; }
