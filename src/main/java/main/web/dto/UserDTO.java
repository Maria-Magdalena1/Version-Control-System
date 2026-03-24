package main.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import main.entities.Role;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDTO {

    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String password;
    private String confirmPassword;
    private LocalDateTime createdAt;
    private boolean isActive;
}
