package main.entities;

import lombok.Getter;

@Getter
public enum VersionStatus {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayValue;

    VersionStatus(String displayValue) {
        this.displayValue = displayValue;
    }
}
