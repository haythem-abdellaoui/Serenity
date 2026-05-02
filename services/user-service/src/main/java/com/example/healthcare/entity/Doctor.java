package com.example.healthcare.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor // nécessaire pour Jackson
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Doctor extends User {

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

}