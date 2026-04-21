package main.ui;

import main.entities.DocumentFile;
import main.entities.Role;
import main.entities.User;
import main.exceptions.*;
import main.services.*;
import main.web.DocumentDTO;
import main.web.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Component
public class ConsoleRunner implements CommandLineRunner {

    private final UserService userService;
    private final DocumentService documentService;
    private final ApprovalService approvalService;
    private final DocumentFileService documentFileService;

    private User loggedInUser = null;
    private final Scanner scanner = new Scanner(System.in);

    @Autowired
    public ConsoleRunner(UserService userService, DocumentService documentService, ApprovalService approvalService, DocumentFileService documentFileService) {
        this.userService = userService;
        this.documentService = documentService;
        this.approvalService = approvalService;
        this.documentFileService = documentFileService;
    }

    @Override
    public void run(@org.jspecify.annotations.NonNull String... args) {

        boolean running = true;
        while (running) {
            if (loggedInUser == null) {
                running = showGuestMenu();
            } else {
                showUserMenu();
            }
        }
    }

    private boolean showGuestMenu() {
        System.out.println("\n1. Register");
        System.out.println("2. Login");
        System.out.println("0. Exit");
        System.out.print("Choose: ");

        switch (scanner.nextLine()) {
            case "1" -> handleRegister();
            case "2" -> handleLogin();
            case "0" -> {
                System.out.println("Goodbye!");
                return false;
            }
            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    private void showUserMenu() {
        System.out.println("\n=== Logged in as: " + loggedInUser.getUsername()
                + " [" + loggedInUser.getRole().getDisplayName() + "] ===");

        Role role = loggedInUser.getRole();

        System.out.println("--- Documents ---");
        if (role == Role.AUTHOR) {
            System.out.println("1. Create document");
            System.out.println("2. Create new version");
            System.out.println("3. Submit version for review");
        }

        if (role == Role.REVIEWER || role == Role.READER || role == Role.AUTHOR) {

            System.out.println("4. View version history");
            System.out.println("5. Compare versions");
            System.out.println("6. View active version");
            System.out.println("7. View approved versions");
            System.out.println("8. Export active version to PDF");
        }

        if (role == Role.AUTHOR) {
            System.out.println("--- Files ---");
            System.out.println("9. Upload file to document");
            System.out.println("10. Delete file");
        }
        if (role == Role.REVIEWER || role == Role.READER || role == Role.AUTHOR) {
            System.out.println("11. View files for document");
        }
        if (role == Role.REVIEWER) {
            System.out.println("--- Review ---");
            System.out.println("12. Review version");
        }
        if (role == Role.ADMINISTRATOR) {
            System.out.println("--- Admin ---");
            System.out.println("13. Change user role");
            System.out.println("14. Activate user");
            System.out.println("15. Deactivate user");
        }

        System.out.println("--- Account ---");
        System.out.println("16. Change password");
        System.out.println("0. Logout");
        System.out.print("Choose: ");

        switch (scanner.nextLine()) {
            case "1" -> {
                if (role != Role.AUTHOR) System.out.println("Access denied.");
                else handleCreateDocument();
            }
            case "2" -> {
                if (role != Role.AUTHOR) System.out.println("Access denied.");
                else handleCreateNewVersion();
            }
            case "3" -> {
                if (role != Role.AUTHOR) System.out.println("Access denied.");
                else handleSubmitForReview();
            }
            case "4" -> handleVersionHistory();
            case "5" -> handleCompareVersions();
            case "6" -> handleGetActiveVersion();
            case "7" -> handleGetApprovedVersions();
            case "8" -> handleExportToPdf();
            case "9" -> {
                if (role != Role.AUTHOR) System.out.println("Access denied.");
                else handleUploadFile();
            }
            case "10" -> {
                if (role != Role.AUTHOR) System.out.println("Access denied.");
                else handleDeleteFile();
            }
            case "11" -> handleGetFiles();
            case "12" -> {
                if (role != Role.REVIEWER) System.out.println("Access denied.");
                else handleReviewVersion();
            }
            case "13" -> {
                if (role != Role.ADMINISTRATOR) System.out.println("Access denied.");
                else handleChangeUserRole();
            }
            case "14" -> {
                if (role != Role.ADMINISTRATOR) System.out.println("Access denied.");
                else handleActivateUser();
            }
            case "15" -> {
                if (role != Role.ADMINISTRATOR) System.out.println("Access denied.");
                else handleDeactivateUser();
            }
            case "16" -> handleChangePassword();
            case "0" -> {
                loggedInUser = null;
                System.out.println("Logged out.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private void handleRegister() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Full name: ");
        String fullName = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Confirm password: ");
        String confirmPassword = scanner.nextLine();

        try {
            userService.register(UserDTO.builder()
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .password(password)
                    .confirmPassword(confirmPassword)
                    .build());
            System.out.println("Registered successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleLogin() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            userService.login(username, password);
            loggedInUser = userService.findByUsername(username);
            System.out.println("Welcome, " + loggedInUser.getUsername() + "!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleCreateDocument() {
        if (loggedInUser.getRole() != Role.AUTHOR) {
            System.out.println("Access denied: Only authors can create documents.");
            return;
        }
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Description: ");
        String description = scanner.nextLine();
        System.out.print("Content: ");
        String content = scanner.nextLine();
        System.out.print("File path (optional, press Enter to skip): ");
        String filePath = scanner.nextLine();
        try {
            documentService.create(DocumentDTO.builder()
                    .title(title)
                    .description(description)
                    .content(content)
                    .filePath(filePath.isBlank() ? null : filePath)
                    .build(), loggedInUser);
            System.out.println("Document created successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleCreateNewVersion() {
        UUID documentId = readUUID("Document ID: ");
        System.out.print("Change type (major/minor/patch): ");
        String changeType = scanner.nextLine();
        System.out.print("Content: ");
        String content = scanner.nextLine();
        System.out.print("File path (optional, press Enter to skip): ");
        String filePath = scanner.nextLine();
        try {
            //documentversionservice
            documentService.createNewVersion(documentId, changeType, content,
                    filePath.isBlank() ? null : filePath);
            System.out.println("New version created successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleSubmitForReview() {
        UUID versionId = readUUID("Version ID: ");

        try {
            documentService.submitForReview(versionId, loggedInUser);
            System.out.println("Version submitted for review!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleVersionHistory() {
        UUID documentId = readUUID("Document ID: ");

        try {
            documentService.getVersionHistory(documentId).forEach(v ->
                    System.out.printf("Version: %s | Status: %s | Author: %s | Date: %s%n",
                            v.getVersionNumber(),
                            v.getStatus().getDisplayValue(),
                            v.getCreatedBy().getUsername(),
                            v.getCreatedAt())
            );
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleCompareVersions() {
        UUID versionId1 = readUUID("Version ID 1: ");
        UUID versionId2 = readUUID("Version ID 2: ");

        try {
            var result = documentService.compareVersions(versionId1, versionId2);
            System.out.println("\n--- Version 1 ---");
            System.out.println("Version: " + result.getVersionNumber1());
            System.out.println("Author:  " + result.getAuthor1());
            System.out.println("Date:    " + result.getCreatedAt1());
            System.out.println("Status:  " + result.getStatus1());
            System.out.println("\n--- Version 2 ---");
            System.out.println("Version: " + result.getVersionNumber2());
            System.out.println("Author:  " + result.getAuthor2());
            System.out.println("Date:    " + result.getCreatedAt2());
            System.out.println("Status:  " + result.getStatus2());
            System.out.println("\n--- Added lines ---");
            result.getAddedLines().forEach(l -> System.out.println("+ " + l));
            System.out.println("\n--- Removed lines ---");
            result.getRemovedLines().forEach(l -> System.out.println("- " + l));
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleGetActiveVersion() {
        UUID documentId = readUUID("Document ID: ");

        try {
            var version = documentService.getActiveVersion(documentId);
            System.out.printf("Active version: %s | Author: %s | Date: %s%n",
                    version.getVersionNumber(),
                    version.getCreatedBy().getUsername(),
                    version.getCreatedAt());
            System.out.println("Content:\n" + version.getContent());
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleGetApprovedVersions() {
        UUID documentId = readUUID("Document ID: ");

        try {
            documentService.getApprovedVersions(documentId).forEach(v ->
                    System.out.printf("Version: %s | Author: %s | Date: %s%n",
                            v.getVersionNumber(),
                            v.getCreatedBy().getUsername(),
                            v.getCreatedAt())
            );
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleExportToPdf() {
        UUID documentId = readUUID("Document ID: ");

        System.out.print("Save path (e.g. /home/user/document.pdf): ");
        String path = scanner.nextLine();

        try {
            byte[] pdf = documentService.exportToPdf(documentId);
            Files.write(Path.of(path), pdf);
            System.out.println("PDF exported to: " + path);
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleUploadFile() {
        UUID documentId = readUUID("Document ID: ");
        System.out.print("File path: ");
        String filePath = scanner.nextLine();

        try {
            var version = documentService.getActiveVersion(documentId);
            var document = version.getDocument();
            documentFileService.uploadFile(filePath, document, version, loggedInUser);
            System.out.println("File uploaded successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleDeleteFile() {
        UUID fileId = readUUID("File ID: ");
        UUID documentId = readUUID("Document ID: ");

        try {
            var version = documentService.getActiveVersion(documentId);
            documentFileService.deleteFile(fileId, version, loggedInUser);
            System.out.println("File deleted successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleGetFiles() {
        UUID documentId = readUUID("Document ID: ");

        try {
            List<DocumentFile> files =
                    documentFileService.getFilesByDocument(documentId);

            if (files.isEmpty()) {
                System.out.println("No files found for this document.");
                return;
            }

            files.forEach(f ->
                    System.out.printf(
                            "File: %s | Size: %d bytes | Uploaded by: %s | Date: %s%n",
                            f.getFileName(),
                            f.getFileSizeBytes(),
                            f.getUploadedBy().getUsername(),
                            f.getUploadedAt()
                    )
            );

        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleReviewVersion() {
        UUID versionId = readUUID("Version ID: ");
        System.out.print("Approve? (yes/no): ");
        boolean isApproved = scanner.nextLine().equalsIgnoreCase("yes");
        System.out.print("Comment: ");
        String comment = scanner.nextLine();

        try {
            approvalService.reviewVersion(versionId, isApproved, comment, loggedInUser);
            System.out.println("Version " + (isApproved ? "approved" : "rejected") + "!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleChangeUserRole() {
        UUID userId = readUUID("User ID: ");
        System.out.println("Roles: AUTHOR, REVIEWER, READER, ADMINISTRATOR");
        System.out.print("New role: ");
        Role newRole = Role.valueOf(scanner.nextLine().toUpperCase());
        System.out.println(loggedInUser);
        try {
            userService.changeUserRole(userId, newRole);
            System.out.println("Role changed successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleActivateUser() {
        UUID userId = readUUID("User ID: ");

        try {
            userService.activate(userId, loggedInUser.getUserId());
            System.out.println("User activated!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleDeactivateUser() {
        UUID userId = readUUID("User ID: ");

        try {
            userService.deactivate(userId, loggedInUser.getUserId());
            System.out.println("User deactivated!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleChangePassword() {
        System.out.print("Current password: ");
        String oldPassword = scanner.nextLine();
        System.out.print("New password: ");
        String newPassword = scanner.nextLine();

        try {
            userService.changePassword(loggedInUser.getUserId(), oldPassword, newPassword);
            System.out.println("Password changed successfully!");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private UUID readUUID(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return UUID.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid UUID format. Try again.");
            }
        }
    }

    private void handleError(Exception e) {
        if (e instanceof EntityAlreadyExistsException) {
            System.out.println("Already exists: " + e.getMessage());
        } else if (e instanceof UnauthorizedException) {
            System.out.println("Access denied: " + e.getMessage());
        } else if (e instanceof InvalidVersionStatusException) {
            System.out.println("Invalid status: " + e.getMessage());
        } else if (e instanceof DocumentNotFoundException) {
            System.out.println("Not found: " + e.getMessage());
        } else if (e instanceof VersionNotFoundException) {
            System.out.println("Version not found: " + e.getMessage());
        } else if (e instanceof ActiveVersionNotFoundException) {
            System.out.println("No active version: " + e.getMessage());
        } else if (e instanceof ApprovedVersionNotFoundException) {
            System.out.println("No approved version: " + e.getMessage());
        } else if (e instanceof IncorrectPasswordException) {
            System.out.println("Wrong password: " + e.getMessage());
        } else if (e instanceof PasswordsDoNotMatchException) {
            System.out.println("Passwords do not match: " + e.getMessage());
        } else if (e instanceof DisabledException) {
            System.out.println("Account disabled: " + e.getMessage());
        } else if (e instanceof BadCredentialsException) {
            System.out.println("Invalid credentials: " + e.getMessage());
        } else if (e instanceof IllegalStateException) {
            System.out.println("Invalid operation: " + e.getMessage());
        } else if (e instanceof IllegalArgumentException) {
            System.out.println("Invalid input: " + e.getMessage());
        } else {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}
