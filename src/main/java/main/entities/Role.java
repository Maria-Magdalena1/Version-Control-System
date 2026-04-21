package main.entities;

import lombok.Getter;

@Getter
public enum Role {
    AUTHOR("Author"),
    REVIEWER("Reviewer"),
    READER("Reader"),
    ADMINISTRATOR("Administrator"),;

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }
}
