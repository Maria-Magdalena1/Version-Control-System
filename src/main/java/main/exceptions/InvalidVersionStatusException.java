package main.exceptions;

public class InvalidVersionStatusException extends RuntimeException {
    public InvalidVersionStatusException(String message) {
        super(message);
    }
}
