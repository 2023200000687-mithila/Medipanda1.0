package com.medipanda.controller;

import com.medipanda.config.JwtService; import com.medipanda.model.User; import com.medipanda.repository.UserRepository; import com.medipanda.repository.PharmacyRepository; import com.medipanda.repository.RiderRepository; import com.medipanda.model.Pharmacy; import com.medipanda.model.Rider;
import lombok.Data; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
import java.security.Principal; import java.util.Set; import org.springframework.web.multipart.MultipartFile; import java.io.IOException; import java.util.Base64;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final PharmacyRepository pharmacies; private final RiderRepository riders; private final com.medipanda.service.OtpService otp; private final com.medipanda.service.CloudinaryService cloudinary;
 public AuthController(UserRepository users,PasswordEncoder encoder,JwtService jwt,PharmacyRepository pharmacies,RiderRepository riders,com.medipanda.service.OtpService otp, com.medipanda.service.CloudinaryService cloudinary){this.users=users;this.encoder=encoder;this.jwt=jwt;this.pharmacies=pharmacies;this.riders=riders;this.otp=otp;this.cloudinary=cloudinary;}
 @PostMapping("/login") public LoginResponse login(@RequestBody LoginRequest req){
  if(blank(req.identifier)||blank(req.password)) bad("Phone/email and password are required");
  User u=users.findByEmailIgnoreCaseOrPhone(req.identifier.trim(),req.identifier.trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid phone/email or password"));
  if(!u.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account is pending admin approval or disabled");
  if(u.getEmail()!=null && !u.isEmailVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Please verify your email before logging in");
  if(!encoder.matches(req.password,u.getPassword())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid phone/email or password");
  return response(u);
 }
 @PostMapping("/register") public LoginResponse register(@RequestBody RegisterRequest req){
  String role=blank(req.role)?"CUSTOMER":req.role.toUpperCase();
  if(!Set.of("CUSTOMER","PHARMACY","RIDER").contains(role)) bad("Invalid account type");
  if(blank(req.name)||blank(req.password)) bad("Name and password are required");
  if(req.password.length()<8) bad("Password must be at least 8 characters");
  String email=blank(req.email)?null:req.email.trim().toLowerCase(); String phone=blank(req.phone)?null:req.phone.trim();
  if(email==null) bad("Gmail/email is required for registration and OTP verification");
  if(email!=null&&!email.matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) bad("Please use a valid Gmail address");
  if(email!=null&&users.findByEmailIgnoreCase(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered");
  if(phone!=null&&users.findByPhone(phone).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Phone already registered");
  if(("PHARMACY".equals(role)||"RIDER".equals(role))&&blank(req.verificationDocument)) bad("NID or Trade Licence document is required");
  if(("PHARMACY".equals(role)||"RIDER".equals(role))&&email==null) bad("A valid email is required for pharmacy/rider OTP verification");
  if(email!=null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) bad("Please provide a valid email address");
  String status="CUSTOMER".equals(role)?(email==null?"APPROVED":"EMAIL_PENDING"):"PENDING";
  if(email!=null) {
   try { otp.send(email,"REGISTRATION"); }
   catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
   catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage()); }
  }
  User u=users.save(User.builder().name(req.name.trim()).email(email).phone(phone).password(encoder.encode(req.password)).role(role).enabled("APPROVED".equals(status))
   .emailVerified(email==null).verificationStatus(status).verificationDocument(req.verificationDocument).verificationDocumentType(req.verificationDocumentType).build());
  if("PHARMACY".equals(role)) pharmacies.save(Pharmacy.builder().name(blank(req.businessName)?req.name.trim():req.businessName.trim()).area(blank(req.area)?"Dhaka":req.area.trim()).street(blank(req.street)?"":req.street.trim()).image("https://images.unsplash.com/photo-1585435557343-3b092031a831?w=1000").rating("New").deliveryTime("25-40 min").description("Pending verification").open(false).latitude(23.7806).longitude(90.4070).locationAddress((blank(req.street)?"":req.street.trim())+", "+(blank(req.area)?"Dhaka":req.area.trim())+", Bangladesh").ownerUserId(u.getId()).build());
  if("RIDER".equals(role)) riders.save(Rider.builder().userId(u.getId()).name(req.name.trim()).phone(phone).vehicle(blank(req.vehicle)?"Bike":req.vehicle).status("OFFLINE").latitude(23.7806).longitude(90.4070).locationAddress("Dhaka, Bangladesh").earnings(0).build());
  return response(u);
 }
 @PostMapping(value="/register", consumes="multipart/form-data") public LoginResponse registerMultipart(@RequestParam String name, @RequestParam(required=false) String email, @RequestParam(required=false) String phone, @RequestParam String password, @RequestParam(required=false,defaultValue="CUSTOMER") String role, @RequestParam(required=false) String businessName, @RequestParam(required=false) String area, @RequestParam(required=false) String street, @RequestParam(required=false) String vehicle, @RequestPart(required=false) MultipartFile verificationDocument) throws IOException {
  RegisterRequest req=new RegisterRequest(); req.name=name; req.email=email; req.phone=phone; req.password=password; req.role=role; req.businessName=businessName; req.area=area; req.street=street; req.vehicle=vehicle;
  if(verificationDocument!=null && !verificationDocument.isEmpty()){ long max=1024L*1024L*2L; if(verificationDocument.getSize()>max) bad("Verification document must be 2 MB or smaller"); String ct=verificationDocument.getContentType(); if(ct==null || !(ct.startsWith("image/") || "application/pdf".equalsIgnoreCase(ct))) bad("Only image or PDF documents are allowed"); req.verificationDocument=cloudinary.upload(verificationDocument, "verification"); req.verificationDocumentType="RIDER".equalsIgnoreCase(role)?"NID":"TRADE_LICENSE"; }
  return register(req);
 }
 @PostMapping("/verify-email") public LoginResponse verifyEmail(@RequestBody OtpVerifyRequest req){
  if(blank(req.email)||blank(req.code)) bad("Email and OTP are required");
  if(!otp.verify(req.email,"REGISTRATION",req.code)) bad("Invalid or expired OTP");
  User u=users.findByEmailIgnoreCase(req.email.trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration not found"));
  u.setEmailVerified(true);
  if("CUSTOMER".equals(u.getRole())) { u.setVerificationStatus("APPROVED"); u.setEnabled(true); }
  else { u.setVerificationStatus("PENDING"); u.setEnabled(false); }
  User saved=users.save(u);
  return response(saved);
 }
 @PostMapping("/resend-otp") public java.util.Map<String,Object> resendOtp(@RequestBody java.util.Map<String,String> body){
  String email=body.get("email"); if(blank(email)) bad("Email is required");
  try{otp.send(email,"REGISTRATION");return java.util.Map.of("success",true,"message","A new OTP was sent");}
  catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
  catch(IllegalStateException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage());}
 }
 @PostMapping("/forgot-password") public java.util.Map<String,Object> forgotPassword(@RequestBody ForgotPasswordRequest req){
  if(blank(req.email)) bad("Gmail/email is required");
  String email=req.email.trim().toLowerCase();
  // Do not reveal whether an account exists.
  users.findByEmailIgnoreCase(email).ifPresent(u->{
   try { otp.send(email,"PASSWORD_RESET"); }
   catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
   catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage()); }
  });
  return java.util.Map.of("success",true,"message","If an account exists for this email, a password reset OTP has been sent.");
 }
 @PostMapping("/reset-password") public java.util.Map<String,Object> resetPassword(@RequestBody ResetPasswordRequest req){
  if(blank(req.email)||blank(req.code)||blank(req.newPassword)) bad("Email, OTP and new password are required");
  if(req.newPassword.length()<8) bad("New password must be at least 8 characters");
  String email=req.email.trim().toLowerCase();
  if(!otp.verify(email,"PASSWORD_RESET",req.code)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired OTP");
  User u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid reset request"));
  u.setPassword(encoder.encode(req.newPassword)); u.setEmailVerified(true); if("CUSTOMER".equals(u.getRole())&&!u.isEnabled()&&!"REJECTED".equals(u.getVerificationStatus())){u.setEnabled(true);u.setVerificationStatus("APPROVED");} users.save(u);
  return java.util.Map.of("success",true,"message","Password reset successful. Please sign in with your new password.");
 }
 @GetMapping("/me") public User me(Principal p){return users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));}
 @PatchMapping("/me") public User updateMe(Principal p,@RequestBody UpdateRequest req){User u=users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow();if(!blank(req.name))u.setName(req.name.trim());if(!blank(req.password)){if(req.password.length()<8)bad("Password must be at least 8 characters");u.setPassword(encoder.encode(req.password));}return users.save(u);}
 @PatchMapping(value="/me/profile", consumes="multipart/form-data") public ProfileResponse updateProfile(Principal p,@RequestParam(required=false) String name,@RequestParam(required=false) String email,@RequestParam(required=false) String password,@RequestPart(required=false) MultipartFile profilePicture) throws IOException {
  User u=users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow();
  if(!blank(name)) u.setName(name.trim());
  if(!blank(email)){String e=email.trim().toLowerCase();if(!e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) bad("Please provide a valid email address");if(!e.equalsIgnoreCase(u.getEmail())&&users.findByEmailIgnoreCase(e).filter(x->!x.getId().equals(u.getId())).isPresent()) bad("Email already registered");if(!e.equalsIgnoreCase(u.getEmail())){otp.send(e,"REGISTRATION");u.setEmailVerified(false);}u.setEmail(e);}
  if(!blank(password)){if(password.length()<8)bad("Password must be at least 8 characters");u.setPassword(encoder.encode(password));}
  if(profilePicture!=null&&!profilePicture.isEmpty()){if(profilePicture.getSize()>1024L*1024L)bad("Profile picture must be 1 MB or smaller");String ct=profilePicture.getContentType();if(ct==null||!ct.startsWith("image/"))bad("Profile picture must be an image");u.setProfilePicture(cloudinary.upload(profilePicture, "profile"));}
  User saved=users.save(u); return new ProfileResponse(saved,response(saved));
 }
 private LoginResponse response(User u){return new LoginResponse(jwt.generate(u.getEmail()!=null?u.getEmail():u.getPhone(),u.getRole()),u.getName(),u.getEmail(),u.getPhone(),u.getRole(),u.getId(),u.getVerificationStatus());}
 private static boolean blank(String s){return s==null||s.isBlank();} private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 @Data public static class LoginRequest{private String identifier;private String password;}
 @Data public static class RegisterRequest{private String name,email,phone,password,role,verificationDocument,verificationDocumentType,businessName,area,street,vehicle;}
 @Data public static class UpdateRequest{private String name,password;} @Data public static class OtpVerifyRequest{private String email,code;} @Data public static class ForgotPasswordRequest{private String email;} @Data public static class ResetPasswordRequest{private String email,code,newPassword;}
 public record LoginResponse(String token,String name,String email,String phone,String role,String id,String verificationStatus){}
 public record ProfileResponse(User user, LoginResponse session){}
}
