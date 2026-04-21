package main.services;

import jakarta.transaction.Transactional;
import main.entities.Role;
import main.entities.User;
import main.exceptions.*;
import main.repositories.UserRepository;
import main.web.UserDTO;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    private boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    private void saveUser(User user) {
        userRepository.save(user);
    }


    public void register(UserDTO userDto) {

        if (userDto.getUsername().length() < 3 || userDto.getUsername().length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }

        if (userDto.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (!userDto.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }

        if (usernameExists(userDto.getUsername())) {
            throw new EntityAlreadyExistsException("Username already exists");
        }

        if (emailExists(userDto.getEmail())) {
            throw new EntityAlreadyExistsException("Email already exists");
        }

        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            throw new PasswordsDoNotMatchException("Passwords do not match");
        }

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

        auditLogService.createLogForUser(user, "USER REGISTERED", user,
                "Registered User: " + userDto.getUsername());
    }

    public void login(String username, String password) {
        User user = getUserByUsername(username);

        if (!user.isActive()) {
            throw new DisabledException("User is disabled");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        auditLogService.createLogForUser(user, "USER LOGGED IN", user, "Login User: " + username);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void changeUserRole(UUID userId, Role newRole) {
        User user = getUserById(userId);
        //User admin = getUserById(adminId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminUsername = auth.getName();

        User admin = getUserByUsername(adminUsername);

        Role oldRole = user.getRole();

        user.setRole(newRole);
        user.setUpdatedBy(admin.getUserId());
        user.setUpdatedAt(LocalDateTime.now());

        saveUser(user);

        auditLogService.createLogForUser(admin, "CHANGE ROLE", user, String.format("Changed role from %s to %s", oldRole, newRole));
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

        auditLogService.createLogForUser(user, "CHANGE PASSWORD", user, "User changed their own password");
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(UUID userId, UUID adminId) {
        User user = getUserById(userId);

        User admin = getUserById(adminId);

        if (!user.isActive()) {
            auditLogService.createLogForUser(admin, "USER ALREADY DEACTIVATED", user, "User deactivated");
            throw new IllegalStateException("User is already deactivated");
        } else {
            user.setActive(false);
            user.setUpdatedBy(adminId);
            user.setUpdatedAt(LocalDateTime.now());
            saveUser(user);

            auditLogService.createLogForUser(admin, "DEACTIVATE USER", user, "Deactivate user " + user.getUsername());
        }
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void activate(UUID userId, UUID adminId) {
        User user = getUserById(userId);
        User admin = getUserById(adminId);

        if (user.isActive()) {
            auditLogService.createLogForUser(admin, "USER ALREADY ACTIVATED", user, "User activated");
            throw new IllegalStateException("User is already activated");
        } else {
            user.setActive(true);
            user.setUpdatedBy(adminId);
            user.setUpdatedAt(LocalDateTime.now());
            saveUser(user);

            auditLogService.createLogForUser(admin, "ACTIVATE USER", user, "Activate user " + user.getUsername());
        }
    }

    public User findByUsername(String username) {
        return getUserByUsername(username);
    }

    @NullMarked
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = getUserByUsername(username);

        if (!user.isActive()) {
            throw new DisabledException("This account has been deactivated");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
