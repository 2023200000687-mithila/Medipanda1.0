package com.medipanda.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medipanda.config.JwtService;
import com.medipanda.model.User;
import com.medipanda.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@RestController @RequestMapping("/api/integrations")
public class IntegrationController {
 private final String googleClientId,firebaseWebConfig,vapidKey;
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final ObjectMapper mapper = new ObjectMapper();
 public IntegrationController(@Value("${app.fcm.web-config-json:}") String firebaseWebConfig,@Value("${app.fcm.vapid-key:}") String vapidKey,@Value("${app.integrations.google-client-id:}") String googleClientId,UserRepository users,PasswordEncoder encoder,JwtService jwt){this.firebaseWebConfig=firebaseWebConfig;this.vapidKey=vapidKey;this.googleClientId=googleClientId;this.users=users;this.encoder=encoder;this.jwt=jwt;}
 @GetMapping("/config") public Map<String,Object> config(){Map<String,Object> m=new HashMap<>();m.put("googleClientId",googleClientId);m.put("googleEnabled",!googleClientId.isBlank());boolean push=!firebaseWebConfig.isBlank()&&!vapidKey.isBlank();m.put("pushEnabled",push);if(push){try{m.put("firebase",mapper.readTree(firebaseWebConfig));m.put("vapidKey",vapidKey);}catch(Exception e){m.put("pushEnabled",false);}}return m;}
 @PostMapping("/google-login") public Map<String,Object> googleLogin(@RequestBody Map<String,String> body){String credential=body.get("credential");if(credential==null||credential.isBlank()||googleClientId.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Google Sign-In is not configured.");try{String url="https://oauth2.googleapis.com/tokeninfo?id_token="+URLEncoder.encode(credential,StandardCharsets.UTF_8);HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();HttpResponse<String> res=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString());if(res.statusCode()!=200)throw new Exception();JsonNode n=mapper.readTree(res.body());if(!googleClientId.equals(n.path("aud").asText())||!"true".equals(n.path("email_verified").asText())||n.path("email").asText().isBlank())throw new Exception();String email=n.path("email").asText().toLowerCase(Locale.ROOT);User u=users.findByEmailIgnoreCase(email).orElseGet(()->users.save(User.builder().name(n.path("name").asText("Google User")).email(email).password(encoder.encode(UUID.randomUUID().toString())).role("CUSTOMER").enabled(true).emailVerified(true).verificationStatus("APPROVED").profilePicture(n.path("picture").asText(null)).build()));if(!u.isEnabled()||!u.isEmailVerified())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This account is pending approval or email verification.");String token=jwt.generate(u.getEmail()!=null?u.getEmail():u.getPhone(),u.getRole());return Map.of("token",token,"name",u.getName()==null?"":u.getName(),"email",u.getEmail()==null?"":u.getEmail(),"phone",u.getPhone()==null?"":u.getPhone(),"role",u.getRole(),"id",u.getId(),"verificationStatus",u.getVerificationStatus()==null?"APPROVED":u.getVerificationStatus());}catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Google sign-in token is invalid or expired.");}}
}
