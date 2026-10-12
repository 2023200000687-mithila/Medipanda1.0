package com.medipanda.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("otp_codes")
public class OtpCode {
    @Id private String id;
    private String email;
    private String code;
    private String purpose;
    private Date expiresAt;
    private Date createdAt;
    private int attempts;
    private boolean used;
}
