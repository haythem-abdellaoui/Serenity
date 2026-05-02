package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.dto.DoctorUpdateRequest;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import com.example.healthcare.mapper.DoctorMapper;
import com.example.healthcare.repository.DoctorRepository;
import com.example.healthcare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorService implements IDoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisPublisher redisPublisher;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private UserService userService;

    @Override
    public DoctorResponseDTO createDoctorForExistingUser(Long userId, String specialty, MultipartFile image) throws IOException {

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (existingUser.getRole() != Role.DOCTOR) {
            throw new RuntimeException("User with id " + userId + " does not have the DOCTOR role");
        }

        String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
        Path filePath = Paths.get("uploads/", fileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = new Doctor();
        doctor.setId(existingUser.getId());
        doctor.setEmail(existingUser.getEmail());
        doctor.setPassword(existingUser.getPassword());
        doctor.setFirstName(existingUser.getFirstName());
        doctor.setLastName(existingUser.getLastName());
        doctor.setPhone(existingUser.getPhone());
        doctor.setDateOfBirth(existingUser.getDateOfBirth());
        doctor.setRole(existingUser.getRole());
        doctor.setAuthProvider(existingUser.getAuthProvider());
        doctor.setIsActive(false);
        doctor.setSpecialty(specialty);
        doctor.setProfilePictureUrl("uploads/" + fileName);

        UserProfile profile = UserProfile.builder()
                .user(doctor)
                .preferredLanguage("en")
                .isAnonymous(false)
                .build();
        doctor.setProfile(profile);


        log("=== WORKING DIR: " + System.getProperty("user.dir"));
        log("=== SAVING TO: " + Paths.get("uploads/" + fileName).toAbsolutePath());

        userRepository.delete(existingUser);
        Doctor savedDoctor = doctorRepository.save(doctor);
        UserResponseDTO updatedUser = userService.uploadAvatar(doctor.getEmail(), image);

        redisPublisher.publishDoctorEvent(doctorMapper.toDTO(savedDoctor));

        return doctorMapper.toDTO(savedDoctor);
    }

    @Override
    public List<DoctorResponseDTO> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(doctorMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DoctorResponseDTO> getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .map(doctorMapper::toDTO);
    }

    @Override
    public DoctorResponseDTO updateDoctor(Long id, Doctor doctorDetails) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));

        if (doctorDetails.getSpecialty() != null) existingDoctor.setSpecialty(doctorDetails.getSpecialty());
        if (doctorDetails.getProfilePictureUrl() != null) existingDoctor.setProfilePictureUrl(doctorDetails.getProfilePictureUrl());
        if (doctorDetails.getFirstName() != null) existingDoctor.setFirstName(doctorDetails.getFirstName());
        if (doctorDetails.getLastName() != null) existingDoctor.setLastName(doctorDetails.getLastName());
        if (doctorDetails.getPhone() != null) existingDoctor.setPhone(doctorDetails.getPhone());

        return doctorMapper.toDTO(doctorRepository.save(existingDoctor));
    }

    @Override
    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
    }

    @Override
    public void Verify(Long id) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
        existingDoctor.setIsActive(true);
        doctorRepository.save(existingDoctor);
    }

    @Override
    public DoctorResponseDTO updateDoctorFull(Long id, DoctorUpdateRequest request) throws IOException {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (request.getFirstName() != null)    doctor.setFirstName(request.getFirstName());
        if (request.getLastName() != null)     doctor.setLastName(request.getLastName());
        if (request.getPhone() != null)        doctor.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null)  doctor.setDateOfBirth(request.getDateOfBirth());

        UserProfile profile = doctor.getProfile();
        if (profile == null) {
            profile = UserProfile.builder()
                    .user(doctor)
                    .build();
        }
        if (request.getAvatarUrl() != null)         profile.setAvatar(request.getAvatarUrl());
        if (request.getBio() != null)               profile.setBio(request.getBio());
        if (request.getPreferredLanguage() != null)  profile.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getIsAnonymous() != null)        profile.setIsAnonymous(request.getIsAnonymous());
        doctor.setProfile(profile);

        if (request.getSpecialty() != null) doctor.setSpecialty(request.getSpecialty());

        MultipartFile image = request.getImage();
        if (image != null && !image.isEmpty()) {
            Files.createDirectories(Paths.get("uploads"));
            String safeName = image.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = UUID.randomUUID() + "-" + safeName;
            Path filePath = Paths.get("uploads/" + filename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            doctor.setProfilePictureUrl("uploads/" + filename);
            UserResponseDTO updatedUser = userService.uploadAvatar(doctor.getEmail(), image);
        }

        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toDTO(saved);
    }

    @Override
    public String getDoctorEmail(Long id){
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));
        return doctor.getEmail();
    }

    private void log(String message) {
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("debug.log"),
                    message + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {}
    }
}