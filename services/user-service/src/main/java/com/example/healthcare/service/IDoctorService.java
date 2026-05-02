package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.dto.DoctorUpdateRequest;
import com.example.healthcare.entity.Doctor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IDoctorService {
    DoctorResponseDTO createDoctorForExistingUser(Long userId, String speciality, MultipartFile image) throws IOException;
    List<DoctorResponseDTO> getAllDoctors();
    Optional<DoctorResponseDTO> getDoctorById(Long id);
    DoctorResponseDTO updateDoctor(Long id, Doctor doctorDetails);
    DoctorResponseDTO updateDoctorFull(Long id, DoctorUpdateRequest request) throws IOException;
    void deleteDoctor(Long id);
    void Verify(Long id);
    String getDoctorEmail(Long id);
}