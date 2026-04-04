package org.icetank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Example configuration POJO.
 * <p>
 * Configurations are saved and loaded to JSON files
 * <p>
 * All fields should be public and mutable.
 * <p>
 * Fields to static inner classes generate nested JSON objects.
 */
public class ExtraPearlLoaderConfig {
    public final PearlLoaderConfig pearlLoader = new PearlLoaderConfig();
    public static class PearlLoaderConfig {
        public boolean enabled = true;
        /** Map of player UUIDs to list of allowed pearl IDs */
        public Map<UUID, List<String>> allowed = new HashMap<>();
        /** Allow the module to load a pearl id that is equal to the username */
        public boolean guessPearlId = true;
        public ApiProvider apiProvider = ApiProvider.MOJANG;
    }

    public enum ApiProvider {
        MOJANG,
        MINETOOLS
    }
}
