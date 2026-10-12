package com.medipanda.config;

import com.medipanda.model.*;
import com.medipanda.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

@Configuration
public class SeedData {
    @Bean CommandLineRunner seed(PharmacyRepository p, MedicineRepository m, RiderRepository r,
                                 UserRepository u, PasswordEncoder encoder,
                                 @Value("${app.demo-seed.enabled:true}") boolean demoSeedEnabled,
                                 @Value("${app.demo-seed.reset-passwords:true}") boolean resetPasswords) { return args -> {
        // Idempotently seed admin, customer, pharmacy and rider accounts (safe to run on every startup).
        if (demoSeedEnabled) {
            String adminPw = System.getenv().getOrDefault("SEED_ADMIN_PASSWORD", "Admin@12345");
            ensureDemoUser(u, encoder, resetPasswords, "admin@medipanda.com", "01700000000", "MediPanda Admin", "ADMIN", adminPw);
            ensureDemoUser(u, encoder, resetPasswords, "customer@medipanda.com", "01700000010", "Demo Customer", "CUSTOMER", "customer123");
            ensureDemoUser(u, encoder, resetPasswords, "pharmacy@medipanda.com", "01700000020", "MediCare Pharmacy", "PHARMACY", "pharmacy123");
            ensureDemoUser(u, encoder, resetPasswords, "popular@medipanda.com", "01700000021", "Popular Pharmacy", "PHARMACY", "pharmacy123");
            ensureDemoUser(u, encoder, resetPasswords, "rahim@medipanda.com", "01700000001", "Rahim Ahmed", "RIDER", "Rider@12345");
            ensureDemoUser(u, encoder, resetPasswords, "karim@medipanda.com", "01700000002", "Karim Hasan", "RIDER", "Rider@12345");
            ensureDemoUser(u, encoder, resetPasswords, "hasan@medipanda.com", "01700000003", "Hasan Ali", "RIDER", "Rider@12345");
        } else if (System.getenv("SEED_ADMIN_PASSWORD") != null && !System.getenv("SEED_ADMIN_PASSWORD").isBlank()) {
            // Production: create only the admin account (password is never reset on later restarts).
            ensureDemoUser(u, encoder, false, System.getenv().getOrDefault("SEED_ADMIN_EMAIL", "admin@medipanda.com"), "01700000000", "MediPanda Admin", "ADMIN", System.getenv("SEED_ADMIN_PASSWORD"));
        }

        Map<String,String> owner = new HashMap<>();
        u.findByEmailIgnoreCase("pharmacy@medipanda.com").ifPresent(x -> owner.put("MediCare Pharmacy", x.getId()));
        u.findByEmailIgnoreCase("popular@medipanda.com").ifPresent(x -> owner.put("Popular Pharmacy", x.getId()));

        if (p.count() == 0) {
            String[][] data = {
                {"MediCare Pharmacy","Dhanmondi","4.9","20-30 min","23.7465","90.3760"},
                {"Popular Pharmacy","Gulshan","4.8","25-35 min","23.7925","90.4078"},
                {"Wellness Pharmacy","Uttara","4.7","25-35 min","23.8759","90.3795"},
                {"City Drug House","Mirpur","4.6","30-40 min","23.8223","90.3654"}
            };
            for (String[] x : data) p.save(Pharmacy.builder().name(x[0]).area(x[1])
                .image("https://images.unsplash.com/photo-1585435557343-3b092031a831?w=800")
                .rating(x[2]).deliveryTime(x[3]).description("Trusted pharmacy with verified products and fast local delivery.")
                .open(true).latitude(Double.parseDouble(x[4])).longitude(Double.parseDouble(x[5])).locationAddress(x[1]+", Dhaka, Bangladesh").ownerUserId(owner.get(x[0])).build());
        }

        if (m.count() < 50) {
            List<Pharmacy> ps=p.findAll();
            String[][] data={
                {"Napa 500mg","Paracetamol 500mg","Fever & Pain","12","Beximco Pharma"},
                {"Napa Extra","Paracetamol + Caffeine","Fever & Pain","30","Beximco Pharma"},
                {"Ace 500mg","Paracetamol 500mg","Fever & Pain","15","Square Pharmaceuticals"},
                {"Ace Plus","Paracetamol + Caffeine","Fever & Pain","32","Square Pharmaceuticals"},
                {"Seclo 20mg","Omeprazole 20mg","Gastric","35","Square Pharmaceuticals"},
                {"Sergel 20mg","Omeprazole 20mg","Gastric","42","Healthcare Pharmaceuticals"},
                {"Esomep 20mg","Esomeprazole 20mg","Gastric","55","Square Pharmaceuticals"},
                {"DP 20mg","Domperidone 20mg","Digestive","42","Square Pharmaceuticals"},
                {"Antacid Plus","Aluminium Hydroxide + Magnesium Hydroxide","Gastric","60","Opsonin Pharma"},
                {"Vitamin C 500mg","Ascorbic Acid 500mg","Vitamins","180","Healthcare Pharmaceuticals"},
                {"Ceevit 500mg","Vitamin C 500mg","Vitamins","95","Square Pharmaceuticals"},
                {"Zinc 20mg","Zinc Sulfate 20mg","Vitamins","80","Renata Limited"},
                {"Calbo-D","Calcium + Vitamin D3","Vitamins","180","Square Pharmaceuticals"},
                {"Fexo 120mg","Fexofenadine 120mg","Allergy & Cold","55","Square Pharmaceuticals"},
                {"Fexo 60mg","Fexofenadine 60mg","Allergy & Cold","35","Square Pharmaceuticals"},
                {"Montair 10mg","Montelukast 10mg","Allergy & Cold","90","Incepta Pharmaceuticals"},
                {"ORS Lemon","Oral Rehydration Salts","Hydration","25","SMC Enterprise"},
                {"ORS Orange","Oral Rehydration Salts","Hydration","25","SMC Enterprise"},
                {"Savlon Antiseptic","Chlorhexidine + Cetrimide","First Aid","120","ACI Limited"},
                {"Dettol Antiseptic","Chloroxylenol","First Aid","165","Reckitt"},
                {"Bandage Roll","Absorbent Dressing","First Aid","75","MediPanda Verified"},
                {"Cotton 100g","Medical Cotton","First Aid","90","MediPanda Verified"},
                {"Surgical Mask 10pcs","3-Ply Medical Mask","Personal Care","80","MediPanda Verified"},
                {"Hand Sanitizer 100ml","Ethyl Alcohol","Personal Care","90","ACI Limited"},
                {"Digital Thermometer","Digital Thermometer","Devices","350","MediPanda Verified"},
                {"BP Monitor","Automatic Blood Pressure Monitor","Devices","1850","MediPanda Verified"},
                {"Glucose Test Strips","Blood Glucose Test Strips","Devices","650","MediPanda Verified"},
                {"Saline Nasal Spray","Sodium Chloride","Allergy & Cold","220","Incepta Pharmaceuticals"},
                {"Cough Syrup","Dextromethorphan","Cough & Cold","110","Renata Limited"},
                {"Honey Lemon Lozenges","Menthol + Honey","Cough & Cold","75","MediPanda Verified"},
                {"Iron Plus","Ferrous + Folic Acid","Vitamins","140","Incepta Pharmaceuticals"},
                {"Multivitamin Daily","Multivitamin + Minerals","Vitamins","280","Renata Limited"},
                {"Omega 3","Fish Oil 1000mg","Vitamins","450","MediPanda Verified"},
                {"Calamine Lotion","Calamine","Skin Care","150","Square Pharmaceuticals"},
                {"Moisturizing Cream","Moisturizer","Skin Care","260","MediPanda Verified"},
                {"Aloe Vera Gel","Aloe Vera","Skin Care","180","MediPanda Verified"},
                {"Antifungal Cream","Clotrimazole 1%","Skin Care","120","Square Pharmaceuticals"},
                {"Paracetamol Syrup","Paracetamol","Kids Care","65","Beximco Pharma"},
                {"Kids ORS","Oral Rehydration Salts","Kids Care","25","SMC Enterprise"},
                {"Baby Wipes","Gentle Cleansing Wipes","Baby Care","240","MediPanda Verified"}
            };
            String[] images={
              "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=700",
              "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=700",
              "https://images.unsplash.com/photo-1603398938378-e54eab446dde?w=700",
              "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=700",
              "https://images.unsplash.com/photo-1559757175-0eb30cd8c063?w=700",
              "https://images.unsplash.com/photo-1607619056574-7b8d3f9c2b45?w=700"
            };
            Set<String> existing=new HashSet<>(); for(Medicine x:m.findAll()) existing.add(x.getName().toLowerCase());
            int idx=0; for(String[] x:data){ if(existing.contains(x[0].toLowerCase())) continue; for(Pharmacy ph:ps){
                m.save(Medicine.builder().name(x[0]).genericName(x[1]).category(x[2]).pharmacyId(ph.getId()).image(images[idx%images.length]).manufacturer(x[4]).price(Double.parseDouble(x[3])).oldPrice(Double.parseDouble(x[3])+15).stock(60+(idx%8)*10).prescriptionRequired(Set.of("Seclo 20mg","Sergel 20mg","Esomep 20mg","Montair 10mg","Antifungal Cream").contains(x[0])).build());
            } idx++; }
        }

        if (demoSeedEnabled) {
            saveRider(r,u,"rahim@medipanda.com","Rahim Ahmed","01700000001",23.7445,90.3770,850);
            saveRider(r,u,"karim@medipanda.com","Karim Hasan","01700000002",23.7550,90.3900,720);
            saveRider(r,u,"hasan@medipanda.com","Hasan Ali","01700000003",23.7350,90.3680,640);
        }

        // Repair legacy accounts that exist without their role-specific workspace record.
        for (User user : u.findAll()) {
            if ("PHARMACY".equals(user.getRole()) && p.findByOwnerUserId(user.getId()).isEmpty()) {
                Pharmacy linked = p.findAll().stream()
                    .filter(ph -> ph.getName() != null && ph.getName().equalsIgnoreCase(user.getName()))
                    .findFirst().orElse(null);
                if (linked != null) {
                    linked.setOwnerUserId(user.getId());
                    p.save(linked);
                } else {
                    p.save(Pharmacy.builder().name(user.getName()).area("Dhaka").street("")
                        .image("https://images.unsplash.com/photo-1585435557343-3b092031a831?w=800")
                        .rating("New").deliveryTime("25-40 min").description("Verified pharmacy and healthcare partner.")
                        .open(user.isEnabled()).latitude(23.7806).longitude(90.4070)
                        .locationAddress("Dhaka, Bangladesh").ownerUserId(user.getId()).build());
                }
            }
            if ("RIDER".equals(user.getRole()) && r.findByUserId(user.getId()).isEmpty()) {
                r.save(Rider.builder().userId(user.getId()).name(user.getName()).phone(user.getPhone())
                    .vehicle("Bike").status("OFFLINE").latitude(23.7806).longitude(90.4070)
                    .locationAddress("Dhaka, Bangladesh").earnings(0).build());
            }
        }
    }; }

    private void saveRider(RiderRepository r, UserRepository u, String email, String name, String phone, double lat, double lon, double earnings) {
        u.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (r.findByUserId(user.getId()).isPresent()) return;
            r.save(Rider.builder().userId(user.getId()).name(name).phone(phone).vehicle("Bike")
                .status("AVAILABLE").latitude(lat).longitude(lon).locationAddress("Dhaka, Bangladesh").earnings(earnings).build());
        });
    }

    private static void ensureDemoUser(UserRepository users, PasswordEncoder encoder, boolean resetPassword, String email,
                                       String phone, String name, String role, String rawPassword) {
        boolean isNew = users.findByEmailIgnoreCase(email).isEmpty();
        User user = users.findByEmailIgnoreCase(email).orElseGet(User::new);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            boolean phoneTaken = users.findByPhone(phone).filter(x -> !email.equalsIgnoreCase(x.getEmail())).isPresent();
            if (!phoneTaken) user.setPhone(phone);
        }
        if (isNew || resetPassword || user.getPassword() == null) user.setPassword(encoder.encode(rawPassword));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setVerificationStatus("APPROVED");
        users.save(user);
    }
}
