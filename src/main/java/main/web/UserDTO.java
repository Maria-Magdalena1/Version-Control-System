package main.web;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import main.entities.Role;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserDTO {

    @Column(unique = true, nullable = false, length = 20)
    @Size(min = 3, max = 20)
    private String username;

    @Column(nullable = false)
    @Email
    private String email;

    @Column(nullable = false)
    private String fullName;

    private Role role;

    @Column(nullable = false)
    @Size(min = 6)
    @NotBlank
    private String password;

    private String confirmPassword;

    private LocalDateTime createdAt;

    private boolean isActive;
}
