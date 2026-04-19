package main.exceptions;

public class ActiveVersionNotFoundException extends RuntimeException {
    public ActiveVersionNotFoundException(String message) {
        super(message);
    }
}
