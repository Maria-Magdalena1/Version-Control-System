package main.entities;

public enum VersionStatus {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayValue;

    public String getDisplayValue() {
        return displayValue;
    }

    VersionStatus(String displayValue) {
        this.displayValue = displayValue;
    }
}
