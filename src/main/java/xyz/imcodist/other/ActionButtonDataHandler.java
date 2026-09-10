package xyz.imcodist.other;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import xyz.imcodist.data.ActionButtonData;
import xyz.imcodist.data.ActionButtonDataJSON;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ActionButtonDataHandler {
    public static List<ActionButtonData> actions = new ArrayList<>();

    private static final String CONFIG_SUBDIR = "quick-menu-config";
    private static final String DEFAULT_FILE_NAME = "quickmenudata.json";
    private static final String LEGACY_FILE_NAME = "quickmenu_data.json";
    private static final String ACTIVE_FILE_NAME = "active-profile.txt";

    private static String currentProfile = DEFAULT_FILE_NAME;

    public static Path getConfigDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dir;
    }

    public static String sanitizeProfileName(String name) {
        if (name == null) return DEFAULT_FILE_NAME;
        name = name.trim();
        // Strip path traversal / separators.
        name = name.replace("/", "").replace("\\", "").replace("..", "");
        if (name.isEmpty()) return DEFAULT_FILE_NAME;
        if (!name.toLowerCase().endsWith(".json")) {
            name = name + ".json";
        }
        return name;
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public static String getCurrentProfileDisplayName() {
        if (currentProfile.toLowerCase().endsWith(".json")) {
            return currentProfile.substring(0, currentProfile.length() - 5);
        }
        return currentProfile;
    }

    public static File getDataFile() {
        return getConfigDir().resolve(currentProfile).toFile();
    }

    public static File getDataFile(String profileName) {
        return getConfigDir().resolve(sanitizeProfileName(profileName)).toFile();
    }

    public static List<String> listProfiles() {
        Path dir = getConfigDir();
        List<String> profiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    profiles.add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (profiles.isEmpty()) {
            profiles.add(currentProfile);
        }

        profiles.sort(String.CASE_INSENSITIVE_ORDER);
        return profiles;
    }

    private static Path getActiveMarkerFile() {
        return getConfigDir().resolve(ACTIVE_FILE_NAME);
    }

    private static void loadActiveProfile() {
        Path marker = getActiveMarkerFile();
        if (Files.isRegularFile(marker)) {
            try {
                String saved = Files.readString(marker).trim();
                if (!saved.isEmpty()) {
                    currentProfile = sanitizeProfileName(saved);
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentProfile = DEFAULT_FILE_NAME;
    }

    private static void saveActiveProfile() {
        try {
            Files.writeString(getActiveMarkerFile(), currentProfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void migrateLegacyIfNeeded() {
        Path dir = getConfigDir();
        Path newFile = dir.resolve(DEFAULT_FILE_NAME);

        if (!Files.exists(newFile)) {
            Path legacyRoot = FabricLoader.getInstance().getConfigDir().resolve(LEGACY_FILE_NAME);
            Path legacySubdir = dir.resolve(LEGACY_FILE_NAME);
            try {
                if (Files.exists(legacyRoot)) {
                    Files.move(legacyRoot, newFile, StandardCopyOption.REPLACE_EXISTING);
                } else if (Files.exists(legacySubdir)) {
                    Files.move(legacySubdir, newFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void initialize() {
        getConfigDir();
        migrateLegacyIfNeeded();
        loadActiveProfile();
        // If saved profile file was deleted, fall back to default.
        if (!getDataFile().exists() && !currentProfile.equals(DEFAULT_FILE_NAME)) {
            // Keep the name but load will produce empty list; don't reset marker yet.
        }
        load();
    }

    public static boolean switchProfile(String profileName) {
        String sanitized = sanitizeProfileName(profileName);
        if (sanitized.equals(currentProfile)) {
            return false;
        }
        currentProfile = sanitized;
        saveActiveProfile();
        load();
        // Create the file on disk so it shows up even if empty.
        if (!getDataFile().exists()) {
            save();
        }
        return true;
    }

    public static boolean createProfile(String profileName) {
        String sanitized = sanitizeProfileName(profileName);
        File f = getDataFile(sanitized);
        try {
            if (!f.exists()) {
                if (f.getParentFile() != null) {
                    //noinspection ResultOfMethodCallIgnored
                    f.getParentFile().mkdirs();
                }
                try (FileWriter w = new FileWriter(f)) {
                    w.write("[]");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return switchProfile(sanitized);
    }

    public static boolean deleteProfile(String profileName) {
        String sanitized = sanitizeProfileName(profileName);
        // Don't delete the active profile.
        if (sanitized.equals(currentProfile)) return false;
        // Don't delete the last remaining profile.
        if (listProfiles().size() <= 1) return false;
        try {
            return Files.deleteIfExists(getConfigDir().resolve(sanitized));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void add(ActionButtonData action) {
        actions.add(action);
        save();
    }
    public static void remove(ActionButtonData action) {
        actions.remove(action);
        save();
    }

    public static void load() {
        actions.clear();
        File file = getDataFile();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ActionButtonDataJSON>>(){}.getType();

        // Load the json from .minecraft/config/quick-menu-config/<profile>.json
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
                List<ActionButtonDataJSON> actionDataJSONS = gson.fromJson(fileReader, listType);

                if (actionDataJSONS == null) return;
                for (ActionButtonDataJSON action : actionDataJSONS) {
                    ActionButtonData data = ActionButtonData.fromJSON(action);
                    if (data != null) actions.add(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        List<ActionButtonDataJSON> actionDataJSONS = new ArrayList<>();

        for (ActionButtonData action : actions) {
            actionDataJSONS.add(action.toJSON());
        }

        Gson gson = new Gson();
        String jsonString = gson.toJson(actionDataJSONS);

        // Save the json to .minecraft/config/quick-menu-config/<profile>.json
        File file = getDataFile();
        if (file.getParentFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        }
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
