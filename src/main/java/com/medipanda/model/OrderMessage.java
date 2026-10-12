package com.medipanda.model;
import lombok.*;
import java.util.Date;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderMessage { private String senderRole, senderName, senderEmail, text; private Date sentAt; }
