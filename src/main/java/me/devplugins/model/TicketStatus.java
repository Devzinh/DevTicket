package me.devplugins.model;

public enum TicketStatus {
    ABERTO("Aberto"),
    EM_ANDAMENTO("Em Andamento"),
    FECHADO("Fechado");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TicketStatus fromString(String status) {
        for (TicketStatus ts : values()) {
            if (ts.name().equalsIgnoreCase(status) || ts.displayName.equalsIgnoreCase(status)) {
                return ts;
            }
        }
        return ABERTO;
    }
}