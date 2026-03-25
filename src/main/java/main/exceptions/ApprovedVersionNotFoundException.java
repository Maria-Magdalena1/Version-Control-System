package main.exceptions;

public class ApprovedVersionNotFoundException extends RuntimeException {
    public ApprovedVersionNotFoundException(String message) {
        super(message);
    }
}
