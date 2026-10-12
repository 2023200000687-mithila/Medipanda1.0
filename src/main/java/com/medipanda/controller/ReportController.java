package com.medipanda.controller;
import com.medipanda.model.*; import com.medipanda.repository.*; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.security.Principal; import java.util.*;
@RestController @RequestMapping("/api/reports")
public class ReportController {
 private final ReportRepository reports; private final UserRepository users;
 public ReportController(ReportRepository r,UserRepository u){reports=r;users=u;}
 @GetMapping public List<Report> mine(Principal p){User u=user(p);return reports.findByUserIdOrderByCreatedAtDesc(u.getId());}
 @PostMapping public Report create(Principal p,@RequestBody Report req){User u=user(p);if(req.getSubject()==null||req.getSubject().isBlank()||req.getMessage()==null||req.getMessage().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Subject and message are required");return reports.save(Report.builder().userId(u.getId()).userName(u.getName()).role(u.getRole()).subject(req.getSubject().trim()).message(req.getMessage().trim()).status("OPEN").createdAt(new Date()).updatedAt(new Date()).build());}
 private User user(Principal p){return users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session expired"));}
}
