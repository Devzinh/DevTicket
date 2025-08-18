package me.devplugins.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class VersionCompatibility {
    
    private static final String SERVER_VERSION = Bukkit.getVersion();
    private static final int MAJOR_VERSION = getMajorVersion();
    private static final int MINOR_VERSION = getMinorVersion();
    
    private static int getMajorVersion() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
        }
        return 18;
    }
    
    private static int getMinorVersion() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("\\.");
            if (parts.length >= 3) {
                String minorPart = parts[2].split("-")[0];
                return Integer.parseInt(minorPart);
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static boolean isVersionSupported() {
        return MAJOR_VERSION >= 18 && MAJOR_VERSION <= 21;
    }
    
    public static Material getPlayerHeadMaterial() {
        return Material.PLAYER_HEAD;
    }
    
    public static Material getColoredGlass(String color) {
        try {
            switch (color.toUpperCase()) {
                case "LIGHT_BLUE":
                    return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                case "GRAY":
                case "GREY":
                    return Material.GRAY_STAINED_GLASS_PANE;
                case "LIGHT_GRAY":
                case "LIGHT_GREY":
                    return Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                default:
                    return Material.GLASS_PANE;
            }
        } catch (Exception e) {
            return Material.GLASS_PANE;
        }
    }

    public static Material getColoredWool(String color) {
        try {
            switch (color.toUpperCase()) {
                case "RED":
                    return Material.RED_WOOL;
                case "GREEN":
                    return Material.GREEN_WOOL;
                case "GRAY":
                case "GREY":
                    return Material.GRAY_WOOL;
                default:
                    return Material.WHITE_WOOL;
            }
        } catch (Exception e) {
            return Material.WHITE_WOOL;
        }
    }

    public static boolean materialExists(String materialName) {
        try {
            Material.valueOf(materialName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Material getMaterialWithFallback(String primary, String fallback) {
        if (materialExists(primary)) {
            return Material.valueOf(primary.toUpperCase());
        } else if (materialExists(fallback)) {
            return Material.valueOf(fallback.toUpperCase());
        } else {
            return Material.STONE;
        }
    }

    public static ItemStack createCompatibleItem(Material material, int amount) {
        try {
            return new ItemStack(material, amount);
        } catch (Exception e) {
            return new ItemStack(Material.STONE, amount);
        }
    }

    public static String getVersionInfo() {
        return String.format(
            "Servidor: %s | Bukkit: %s | Versão Detectada: 1.%d.%d | Compatível: %s",
            SERVER_VERSION,
            Bukkit.getBukkitVersion(),
            MAJOR_VERSION,
            MINOR_VERSION,
            isVersionSupported() ? "Sim" : "Não"
        );
    }
    
    public static boolean isVersion(int major, int minor) {
        return MAJOR_VERSION > major || (MAJOR_VERSION == major && MINOR_VERSION >= minor);
    }

    public static boolean is1_19Plus() {
        return MAJOR_VERSION >= 19;
    }

    public static boolean is1_20Plus() {
        return MAJOR_VERSION >= 20;
    }

    public static boolean is1_21Plus() {
        return MAJOR_VERSION >= 21;
    }
    
    public static Material getVersionSpecificMaterial() {
        if (is1_21Plus()) {
            return Material.EMERALD;
        } else if (is1_20Plus()) {
            return Material.EMERALD;
        } else if (is1_19Plus()) {
            return Material.EMERALD;
        } else {
            return Material.EMERALD;
        }
    }
}