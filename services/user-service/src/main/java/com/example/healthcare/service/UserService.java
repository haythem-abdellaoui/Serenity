package com.example.healthcare.service;

import com.example.healthcare.dto.*;
import com.example.healthcare.entity.BanDuration;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    AuthResponseDTO registerUser(UserRequestDTO request);

    UserResponseDTO updateUserRole(String email, String role);

    UserResponseDTO assignRoleInternally(Long userId, String role);

    AuthResponseDTO login(LoginRequestDTO request);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long id);

    UserResponseDTO getUserByEmail(String email);

    UserResponseDTO updateProfile(String email, ProfileUpdateDTO request);

    UserResponseDTO uploadAvatar(String email, MultipartFile file);

    UserResponseDTO updateUser(Long id, UserRequestDTO request);

    void deactivateUser(Long id);

    void activateUser(Long id);

    void banUser(Long id, BanDuration duration);

    void unbanUser(Long id);

    void deleteUser(Long id);

    List<UserDTO> searchUsers(String query);

    List<UserDTO> getUsersNamesByIds(List<Long> ids);

    List<UserResponseDTO> getDoctors();

    List<UserResponseDTO> getPatients();

    List<UserLookupDTO> lookupDoctors();

    List<UserLookupDTO> lookupPatients(String firstName, String lastName);

    List<UserLookupDTO> lookupUsersByIds(List<Long> ids);
}
