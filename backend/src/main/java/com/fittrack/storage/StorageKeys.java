package com.fittrack.storage;

import java.util.UUID;
import java.util.regex.Pattern;

/** Builds and validates storage keys so callers can't inject paths. */
public final class StorageKeys {

    private static final Pattern SAFE_FOLDER = Pattern.compile("[A-Za-z0-9_\\-]+(/[A-Za-z0-9_\\-]+)*");
    private static final Pattern SAFE_EXT = Pattern.compile("[a-z0-9]{1,5}");
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9_\\-]+(/[A-Za-z0-9_\\-]+)*\\.[a-z0-9]{1,5}");

    /** Prefix for public catalog media (exercise thumbnails, videos). Everything else is private. */
    public static final String CATALOG_PREFIX = "catalog/";

    private StorageKeys() {
    }

    public static String newKey(String folder, String extension) {
        if (!SAFE_FOLDER.matcher(folder).matches() || !SAFE_EXT.matcher(extension).matches()) {
            throw new IllegalArgumentException("Unsafe storage path");
        }
        return folder + "/" + UUID.randomUUID() + "." + extension;
    }

    public static boolean isValidKey(String key) {
        return key != null && key.length() <= 300 && SAFE_KEY.matcher(key).matches() && !key.contains("..");
    }

    public static boolean isCatalog(String key) {
        return isValidKey(key) && key.startsWith(CATALOG_PREFIX);
    }
}
