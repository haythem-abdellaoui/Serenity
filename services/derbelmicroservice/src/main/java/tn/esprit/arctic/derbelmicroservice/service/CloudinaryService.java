package tn.esprit.arctic.derbelmicroservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadBase64Image(String base64Image) {
        try {
            log.info("Uploading base64 image to Cloudinary...");
            // Extract the actual base64 string if it contains the data:image/png;base64, prefix
            String data = base64Image;
            
            // Perform the upload
            Map<?, ?> uploadResult = cloudinary.uploader().upload(data, ObjectUtils.emptyMap());
            String url = (String) uploadResult.get("secure_url");
            
            log.info("Successfully uploaded image to Cloudinary: {}", url);
            return url;
            
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }
}
