package main.entities;

public enum ChangeType {
    ADDED("Added"),
    MODIFIED("Modified"),
    DELETED("Deleted"),
    RENAMED("Renamed");

    private final String displayName;

    public String getDisplayName() {
        return displayName;
    }

    ChangeType(String displayName) {
        this.displayName = displayName;
    }
}
