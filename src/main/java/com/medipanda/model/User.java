package com.medipanda.model;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("users")
public class User {
    @Id private String id;
    private String name;
    private String email;
    private String phone;
    @JsonIgnore private String password;
    private String role;
    private boolean enabled;
    private String verificationStatus;
    private String verificationDocument;
    private String verificationDocumentType;
    private String rejectionReason;
    private boolean emailVerified;
    private String profilePicture;
    @JsonIgnore private java.util.List<String> fcmTokens = new java.util.ArrayList<>();
}
