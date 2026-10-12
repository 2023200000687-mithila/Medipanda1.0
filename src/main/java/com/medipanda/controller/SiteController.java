package com.medipanda.controller;
import com.medipanda.model.SiteSettings; import com.medipanda.repository.SiteSettingsRepository; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/site")
public class SiteController { private final SiteSettingsRepository repo; public SiteController(SiteSettingsRepository r){repo=r;}
 @GetMapping public SiteSettings get(){return repo.findAll().stream().findFirst().orElseGet(()->repo.save(SiteSettings.builder().id("main").siteName("MediPanda").tagline("Medicine delivered better").heroTitle("Medicines from trusted pharmacies, delivered to you.").heroText("Browse verified pharmacies, order safely and track your delivery.").heroVideoUrl("https://cdn.pixabay.com/video/2025/07/06/289881_large.mp4").contactEmail("support@medipanda.com").footerText("Professional medicine delivery for everyday healthcare.").maintenanceMode(false).build()));}
}
