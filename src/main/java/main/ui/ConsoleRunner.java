package main.ui;

import main.services.ApprovalService;
import main.services.DocumentService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ConsoleRunner implements CommandLineRunner {

    private final UserService userService;
    private final DocumentService documentService;
    private final ApprovalService approvalService;

    @Autowired
    public ConsoleRunner(UserService userService, DocumentService documentService, ApprovalService approvalService) {
        this.userService = userService;
        this.documentService = documentService;
        this.approvalService = approvalService;
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
