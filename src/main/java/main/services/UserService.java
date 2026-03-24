package main.services;

import jakarta.transaction.Transactional;
import main.entities.AuditLog;
import main.entities.Role;
import main.entities.User;
import main.exceptions.*;
import main.repositories.AuditLogRepository;
import main.repositories.UserRepository;
import main.web.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    private User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user;
    }

    private User getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user;
    }

    private boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    private boolean emailExists(String email) {
        return userRepository.findByEmail(email);
    }

    private void saveUser(User user) {
        userRepository.save(user);
    }

    private void saveLog(AuditLog log) {
        auditLogRepository.save(log);
    }

    public void register(UserDTO userDto) {

        if (usernameExists(userDto.getUsername())) {
            throw new EntityAlreadyExistsException("Username already exists");
        }

        if (emailExists(userDto.getEmail())) {
            throw new EntityAlreadyExistsException("Email already exists");
        }
//
//        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
//            throw new PasswordsDoNotMatchException("Passwords do not match");
//        }

        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .fullName(userDto.getFullName())
                .role(userRepository.count() == 0 ? Role.ADMINISTRATOR : Role.READER)
                .password(passwordEncoder.encode(userDto.getPassword()))
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        saveUser(user);

        AuditLog log = AuditLog.builder()
                .user(user)
                .action("USER REGISTERED")
                .targetUser(user)
                .details("Register User: " + userDto.getUsername())
                .performedAt(LocalDateTime.now())
                .build();

        saveLog(log);
    }

    public String login(String username, String password) {
        User user = getUserByUsername(username);
        if (!user.isActive()) {
            throw new DisabledException("User is disabled");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        AuditLog log = AuditLog.builder()
                .user(user)
                .action("USER LOGGED IN")
                .targetUser(user)
                .details("Login User: " + user.getUsername())
                .performedAt(LocalDateTime.now())
                .build();

        saveLog(log);
        return "Welcome";
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void changeUserRole(UUID userId, Role newRole, UUID adminId) {
        User user = getUserById(userId);

        User admin = getUserById(adminId);

        Role oldRole = user.getRole();

        user.setRole(newRole);
        user.setUpdatedBy(adminId);
        user.setUpdatedAt(LocalDateTime.now());

        saveUser(user);

        AuditLog log = AuditLog.builder()
                .user(admin)
                .action("CHANGE ROLE")
                .targetUser(user)
                .details(String.format("Changed role from %s to %s", oldRole, newRole))
                .performedAt(LocalDateTime.now())
                .build();
        saveLog(log);
    }

    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);

        if (!user.isActive()) {
            throw new DisabledException("User is disabled");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IncorrectPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(userId);
        saveUser(user);

        AuditLog log = AuditLog.builder()
                .user(user)
                .action("CHANGE PASSWORD")
                .targetUser(user)
                .details("User changed their own password")
                .performedAt(LocalDateTime.now())
                .build();
        saveLog(log);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(UUID userId, UUID adminId) {
        User user = getUserById(userId);

        User admin = getUserById(adminId);

        user.setActive(false);
        user.setUpdatedBy(adminId);
        user.setUpdatedAt(LocalDateTime.now());
        saveUser(user);

        AuditLog log = AuditLog.builder()
                .user(admin)
                .action("DEACTIVATE ROLE")
                .targetUser(user)
                .details("Deactivate user " + user.getUsername())
                .performedAt(LocalDateTime.now())
                .build();

        saveLog(log);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = getUserByUsername(username);

        if (!user.isActive()) {
            throw new DisabledException("This account has been deactivated");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();
    }
}
