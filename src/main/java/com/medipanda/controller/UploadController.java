package com.medipanda.controller;

import com.medipanda.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final CloudinaryService cloudinary;
    public UploadController(CloudinaryService cloudinary) { this.cloudinary = cloudinary; }

    @PostMapping(consumes = "multipart/form-data")
    public Map<String, String> upload(@RequestPart("file") MultipartFile file,
                                      @RequestParam("folder") String folder) {
        try {
            return Map.of("url", cloudinary.upload(file, folder));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary upload failed. Check Cloudinary credentials and try again.");
        }
    }
}
