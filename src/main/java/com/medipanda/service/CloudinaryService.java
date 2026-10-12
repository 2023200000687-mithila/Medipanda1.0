package com.medipanda.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryService(@Value("${cloudinary.cloud-name:}") String cloudName,
                             @Value("${cloudinary.api-key:}") String apiKey,
                             @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.configured = !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret,
                "secure", true));
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        if (!configured) throw new IllegalStateException(
                "Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET.");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please choose a file to upload.");
        Set<String> allowed = Set.of("profile", "verification", "medicines", "pharmacies", "prescriptions");
        if (folder == null || !allowed.contains(folder)) throw new IllegalArgumentException("Invalid upload category.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean pdf = "application/pdf".equals(contentType);
        boolean image = contentType.startsWith("image/");
        if (!image && !(pdf && Set.of("verification", "prescriptions").contains(folder))) {
            throw new IllegalArgumentException("Only image files are allowed here; PDF is accepted for verification/prescription documents.");
        }
        long maxBytes = switch (folder) {
            case "profile" -> 5L * 1024 * 1024;
            case "verification", "prescriptions" -> 10L * 1024 * 1024;
            default -> 8L * 1024 * 1024;
        };
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("File is too large for " + folder + ".");
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "medipanda/" + folder,
                "resource_type", pdf ? "raw" : "image",
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false));
        Object url = result.get("secure_url");
        if (url == null) throw new IOException("Cloudinary did not return a secure URL.");
        return url.toString();
    }
}
