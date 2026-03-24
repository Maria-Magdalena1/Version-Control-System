package main.entities;

public enum Decision {
    APPROVED("Approved"),
    REJECTED("Rejected");
    private final String value;

    public String getValue() {
        return value;
    }

    Decision(String value) {
        this.value = value;
    }
}
