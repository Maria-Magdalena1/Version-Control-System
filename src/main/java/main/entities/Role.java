package main.entities;

public enum Role {
    AUTHOR("Author"),
    REVIEWER("Reviewer"),
    READER("Reader"),
    ADMINISTRATOR("Administrator"),;

    private final String displayName;

    public String getDisplayName()
    {
        return displayName;
    }

    Role(String displayName) {
        this.displayName = displayName;
    }
}
