package cam72cam.universalmodcore;

import java.util.Arrays;

public enum Loader {
    FORGE("forge"),
    NEOFORGE("neoforge"),
    FABRIC("fabric"),
    QUILT("quilt");

    private final String name;

    Loader(String str) {
        this.name = str;
    }

    public static Loader parse(String str) {
        if (str.equalsIgnoreCase(FORGE.name)) {
            return FORGE;
        } else if (str.equalsIgnoreCase(NEOFORGE.name)) {
            return NEOFORGE;
        } else if (str.equalsIgnoreCase(FABRIC.name)) {
            return FABRIC;
        } else if (str.equalsIgnoreCase(QUILT.name)) {
            return QUILT;
        }
        throw new IllegalArgumentException("Mod loader must be one of followings: " + Arrays.toString(Loader.values()));
    }

    @Override
    public String toString() {
        return name;
    }
}
