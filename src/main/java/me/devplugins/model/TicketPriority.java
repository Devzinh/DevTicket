package me.devplugins.model;

public enum TicketPriority {
    BAIXA("Baixa", 1),
    MEDIA("Média", 2),
    ALTA("Alta", 3),
    URGENTE("Urgente", 4);

    private final String displayName;
    private final int level;

    TicketPriority(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public static TicketPriority fromString(String priority) {
        for (TicketPriority tp : values()) {
            if (tp.name().equalsIgnoreCase(priority) || tp.displayName.equalsIgnoreCase(priority)) {
                return tp;
            }
        }
        return MEDIA;
    }
}