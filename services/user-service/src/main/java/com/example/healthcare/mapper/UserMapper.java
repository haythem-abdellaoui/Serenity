package com.example.healthcare.mapper;

import com.example.healthcare.dto.UserRequestDTO;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isPermanentlyBanned", ignore = true)
    @Mapping(target = "bannedUntil", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "profile", ignore = true)
    User toEntity(UserRequestDTO dto);

    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(source = "profile", target = "profile")
    UserResponseDTO toResponseDTO(User user);

    @Mapping(target = "id", ignore = true)
    UserResponseDTO.UserProfileDTO toProfileDTO(UserProfile profile);

    List<UserResponseDTO> toResponseDTOList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isPermanentlyBanned", ignore = true)
    @Mapping(target = "bannedUntil", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "email", ignore = true)
    void updateEntityFromDTO(UserRequestDTO dto, @MappingTarget User user);
}
