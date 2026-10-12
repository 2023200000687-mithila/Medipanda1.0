package com.medipanda.controller;

import com.medipanda.service.OtpService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequestMapping("/api/auth/otp")
public class OtpController {
    private final OtpService otp;
    public OtpController(OtpService otp){this.otp=otp;}
    @PostMapping("/send") public Map<String,Object> send(@RequestBody Map<String,String> body){
        try{otp.send(body.get("email"),body.getOrDefault("purpose","REGISTRATION"));return Map.of("success",true,"message","OTP sent to your email");}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
        catch(IllegalStateException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage());}
    }
    @PostMapping("/verify") public Map<String,Object> verify(@RequestBody Map<String,String> body){
        boolean ok=otp.verify(body.get("email"),body.getOrDefault("purpose","REGISTRATION"),body.get("code"));
        if(!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired OTP");
        return Map.of("success",true,"message","Email verified successfully");
    }
}
