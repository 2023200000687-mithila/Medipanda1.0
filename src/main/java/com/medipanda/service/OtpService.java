package com.medipanda.service;

import com.medipanda.model.OtpCode;
import com.medipanda.repository.OtpCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {
    private final OtpCodeRepository repo; private final EmailService email; private final SecureRandom random=new SecureRandom(); private final boolean brevoEnabled; private final boolean devConsole; private static final Logger log=LoggerFactory.getLogger(OtpService.class);
    public OtpService(OtpCodeRepository repo, EmailService email, @Value("${app.mail.brevo-enabled:true}") boolean brevoEnabled, @Value("${app.otp.dev-console:false}") boolean devConsole){this.repo=repo;this.email=email;this.brevoEnabled=brevoEnabled;this.devConsole=devConsole;}
    public void send(String rawEmail,String purpose){
        String e=rawEmail==null?"":rawEmail.trim().toLowerCase();
        if(e.isBlank()||!e.contains("@")) throw new IllegalArgumentException("A valid email is required for OTP verification");
        var existing=repo.findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(e,purpose);
        if(existing.isPresent() && existing.get().getCreatedAt()!=null && System.currentTimeMillis()-existing.get().getCreatedAt().getTime()<60_000) throw new IllegalArgumentException("Please wait 60 seconds before requesting another OTP");
        boolean mailReady = brevoEnabled && email.isEnabled();
        if(!mailReady && !devConsole) throw new IllegalStateException("Email service is not configured. Set BREVO_ENABLED=true, BREVO_API_KEY and BREVO_SENDER_EMAIL.");
        String code=String.format("%06d",random.nextInt(1_000_000)); Date now=new Date();
        repo.save(OtpCode.builder().email(e).code(code).purpose(purpose).createdAt(now).expiresAt(new Date(now.getTime()+TimeUnit.MINUTES.toMillis(10))).attempts(0).used(false).build());
        if(!mailReady){ log.warn("[DEV OTP] {} code for {} is {} (email not configured; app.otp.dev-console=true)",purpose,e,code); return; }
        try { email.sendOtp(e, code, purpose); }
        catch (RuntimeException ex) {
            if(devConsole){ log.warn("[DEV OTP] Email delivery failed ({}). {} code for {} is {}",ex.getMessage(),purpose,e,code); return; }
            repo.findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(e,purpose).ifPresent(o->{o.setUsed(true);repo.save(o);}); throw ex;
        }
    }
    public boolean verify(String rawEmail,String purpose,String code){
        String e=rawEmail==null?"":rawEmail.trim().toLowerCase();
        OtpCode o=repo.findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(e,purpose).orElse(null);
        if(o==null||o.getExpiresAt()==null||o.getExpiresAt().before(new Date())) return false;
        if(o.getAttempts()>=5) return false;
        o.setAttempts(o.getAttempts()+1);
        boolean ok=o.getCode().equals(code==null?"":code.trim()); if(ok)o.setUsed(true); repo.save(o); return ok;
    }
}
