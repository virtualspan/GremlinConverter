package net.virtualspan;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;

public class Main {
    static void main() {
        // Get spritesheet and sounds folder paths
        Path spriteSheetFolder = null;
        Path soundFolder = null;

        // Prompt user to select
        while (spriteSheetFolder == null || soundFolder == null) {
            // Build the options list dynamically
            List<String> optionList = new ArrayList<>();

            if (spriteSheetFolder == null) optionList.add("Spritesheet Folder");
            if (soundFolder == null) optionList.add("Sounds Folder");
            optionList.add("Cancel");

            Object[] options = optionList.toArray();

            int choice = JOptionPane.showOptionDialog(
                    null,
                    """
                            Select the gremlin folders needed for conversion:
                            The spritesheet folder should resemble `SpriteSheet/Gremlins/<Gremlin-name>`
                            and the sounds folder should resemble `Sounds/<Gremlin-name>`
                            """,
                    "Folder Selection",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            String selected = (String) options[choice];

            switch (selected) {
                case "Spritesheet Folder" -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Select Spritesheet Folder");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        spriteSheetFolder = chooser.getSelectedFile().toPath();
                    } else {
                        userCancel();
                    }
                }

                case "Sounds Folder" -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Select Sounds Folder");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        soundFolder = chooser.getSelectedFile().toPath();
                    } else {
                        userCancel();
                    }
                }

                default -> userCancel();
            }
        }

        // Get folder name for gremlin
        String folderName = spriteSheetFolder.getFileName().toString();
        String normalized = folderName.toLowerCase().replace(" ", "-");

        // Export Folder Paths
        Path exportFolder = Path.of(System.getProperty("user.home"), "ConvertedGremlins");
        Path gremlinFolder = exportFolder.resolve(normalized);
        Path convertedSpriteFolder = gremlinFolder.resolve("sprites");
        Path convertedSoundFolder = gremlinFolder.resolve("sounds");

        // File Paths
        Path originalConfigPath = spriteSheetFolder.resolve("config.txt");
        Path frameCountPath = convertedSpriteFolder.resolve("frame-count.json");
        Path spriteMapPath = convertedSpriteFolder.resolve("sprite-map.json");
        Path emoteConfigPath = convertedSpriteFolder.resolve("emote-config.json");
        Path sfxMapPath = convertedSoundFolder.resolve("sfx-map.json");

        // Config Folder Paths
        Path userConfigDir = Path.of(System.getProperty("user.home"), ".config", "linux-desktop-gremlin");
        Path gremlinsDir = userConfigDir.resolve("gremlins");

    if (!Files.exists(originalConfigPath)) {
        // If config file isn't present, outputs message
        JOptionPane.showMessageDialog(
                null,
                "config.txt was not found.\n" +
                        "Please make sure you are selecting the correct sprite folder.",
                "Missing Config File",
                JOptionPane.ERROR_MESSAGE
        );

        throw new RuntimeException("config.txt not found");
    } else if (!Files.exists(spriteSheetFolder.resolve("Actions/idle.png"))) {
        // If idle.png sprite isn't present, outputs message
        JOptionPane.showMessageDialog(
                null,
                "idle.png was not found in the sprite folder.\n" +
                        "The Gremlin you have chosen is most likely incompatible.",
                "Missing idle.png File",
                JOptionPane.ERROR_MESSAGE
        );

        throw new RuntimeException("Actions/idle.png not found");
    } else {
        Scanner scanner = new Scanner(System.in);

        // Sprite Options for the dropdown menu (only shows options that exist)
        List<String> spriteOptionsBuilder = new ArrayList<>();
        spriteOptionsBuilder.add("default");
        String[] emoteDirs = {
                "Emotes/emote1.png",
                "Emotes/emote2.png",
                "Emotes/emote3.png",
                "Emotes/emote4.png",
                "Actions/click.png"
        };
        for (String option : emoteDirs) {
            Path p = spriteSheetFolder.resolve(option);
            if (Files.exists(p)) {
                spriteOptionsBuilder.add(option);
            }
        }
        spriteOptionsBuilder.add("Actions/idle.png");
        String[] spriteOptions = spriteOptionsBuilder.toArray(new String[0]);

        // Sound Options for the dropdown menu
        String[] soundOptions = {
                "default",
                "emote1.wav",
                "emote3.wav"
        };

        // Declaring variables for below
        String home = System.getProperty("user.home");
        String patSound;
        String emoteSoundChoice = "default";
        String emoteSpriteChoice = "default";
        String patSpriteChoice = "default";
        String emoteSpriteDefault;
        String patSpriteDefault;
        String pokeSprite;
        String walkSound;

        // Edge case scenarios for when some files don't exist to convert
        if (Files.exists(spriteSheetFolder.resolve("Emotes/emote3.png"))) {
            emoteSpriteDefault = "Emotes/emote3.png";
        } else {
            emoteSpriteDefault = "Emotes/emote1.png";
        }

        if (Files.exists(spriteSheetFolder.resolve("Emotes/emote1.png"))) {
            patSpriteDefault = "Emotes/emote1.png";
        } else {
            patSpriteDefault = "Emotes/emote2.png";
        }

        if (Files.exists(spriteSheetFolder.resolve("Actions/click.png"))) {
            pokeSprite = "Actions/click.png";
        } else if (Files.exists(spriteSheetFolder.resolve("Emotes/emote2.png"))) {
            pokeSprite = "Emotes/emote2.png";
        } else {
            pokeSprite = "Emotes/emote1.png";
        }

        if (Files.exists(soundFolder.resolve("walk.wav"))) {
            walkSound = "walk.wav";
        } else {
            walkSound = "run.wav";
        }

        if (Files.exists(soundFolder.resolve("emote1.wav")) && !Files.exists(soundFolder.resolve("emote3.wav"))) {
            emoteSoundChoice = "emote1.wav";
        } else if (Files.exists(soundFolder.resolve("emote3.wav")) && !Files.exists(soundFolder.resolve("emote1.wav"))) {
            emoteSoundChoice = "emote3.wav";
        }

        // Sets pat.wav to the pat sound if available
        if (Files.exists(soundFolder.resolve("pat.wav"))) {
            patSound = "pat.wav";
        } else if (Files.exists(soundFolder.resolve("emote4.wav"))) {
            patSound = "emote4.wav";
        } else {
            patSound = "emote2.wav";
        }

        // Allows user to customize sprites/sounds, but skips if not needed
        if (Files.exists(spriteSheetFolder.resolve("Emotes/emote1.png"))
                || Files.exists(spriteSheetFolder.resolve("Emotes/emote2.png"))
                || Files.exists(spriteSheetFolder.resolve("Emotes/emote3.png"))
                || Files.exists(spriteSheetFolder.resolve("Emotes/emote4.png"))) {
            emoteSpriteChoice = chooseSprite("Choose sprite for the emote animation:\n" +
                    "default = " + emoteSpriteDefault + "\n" +
                    "(directories are shown relative to:\n" + spriteSheetFolder.toString().replaceFirst(home, "~") + ")",
                    "Choose sprite", spriteOptions);

            patSpriteChoice = chooseSprite("Choose sprite for the pat animation:\n" +
                    "default = " + patSpriteDefault + "\n" +
                    "(directories are shown relative to:\n" + spriteSheetFolder.toString().replaceFirst(home, "~") + ")",
                    "Choose sprite", spriteOptions);
        }

        if (Files.exists(soundFolder.resolve("emote1.wav"))
                && Files.exists(soundFolder.resolve("emote3.wav"))) {
            emoteSoundChoice = chooseSprite("Choose sound for the emote animation:\n" +
                     "default = emote1.wav\n" +
                     "(directories are shown relative to:\n" + soundFolder.toString().replaceFirst(home, "~") + ")",
                    "Choose sound", soundOptions);
        }

        // Sets defaults
        if (emoteSpriteChoice.equals("default")) {
            emoteSpriteChoice = emoteSpriteDefault;
        }
        if (patSpriteChoice.equals("default")) {
            patSpriteChoice = patSpriteDefault;
        }
        if (emoteSoundChoice.equals("default")) {
            emoteSoundChoice = "emote1.wav";
        }


        // Make sure the export folders exist
        try {
            Files.createDirectories(exportFolder);
            Files.createDirectories(gremlinFolder);
            Files.createDirectories(convertedSpriteFolder);
            Files.createDirectories(convertedSoundFolder);
        } catch (IOException e) {
            ioExceptionPrompt("Failed to create export directories", e);
        }

        // Paths to directories containing sprites
        Path actions = spriteSheetFolder.resolve("Actions");
        Path run = spriteSheetFolder.resolve("Run");
        Path walk = spriteSheetFolder.resolve("Walk");

        // Copies and renames hover.png or idle.png to intro.png and outro.png as a placeholder if they aren't present
        // This fixes issues with starting/closing the Gremlin and gets overridden if they are present
        String introSprite = "intro.png";
        String outroSprite = "outro.png";

        try {
            if (!Files.exists(actions.resolve("intro.png"))) {
                Files.copy(actions.resolve("idle.png"),
                        convertedSpriteFolder.resolve("intro.png"), StandardCopyOption.REPLACE_EXISTING);

                introSprite = "idle.png";
            }

            if (!Files.exists(actions.resolve("outro.png"))) {
                if (Files.exists(actions.resolve("hover.png"))) {
                    Files.copy(actions.resolve("grab.png"),
                            convertedSpriteFolder.resolve("outro.png"), StandardCopyOption.REPLACE_EXISTING);

                    outroSprite = "grab.png";
                } else {
                    Files.copy(actions.resolve("idle.png"),
                            convertedSpriteFolder.resolve("outro.png"), StandardCopyOption.REPLACE_EXISTING);

                    outroSprite = "idle.png";
                }
            }
        } catch (IOException e) {
            ioExceptionPrompt("Failed to copy and rename to intro/outro.png placeholder files", e);
        }

        // List of copy operations
        List<Path[]> copies = List.of(
                // Actions folder
                new Path[]{actions.resolve("grab.png"), convertedSpriteFolder.resolve("grab.png")},
                new Path[]{actions.resolve("hover.png"), convertedSpriteFolder.resolve("hover.png")},
                new Path[]{actions.resolve("idle.png"), convertedSpriteFolder.resolve("idle.png")},
                new Path[]{actions.resolve(introSprite), convertedSpriteFolder.resolve("intro.png")},
                new Path[]{actions.resolve(outroSprite), convertedSpriteFolder.resolve("outro.png")},
                new Path[]{actions.resolve("runIdle.png"), convertedSpriteFolder.resolve("walk-idle.png")},
                new Path[]{actions.resolve("sleep.png"), convertedSpriteFolder.resolve("sleep.png")},

                // Run folder
                new Path[]{run.resolve("downLeft.png"), convertedSpriteFolder.resolve("run-downleft.png")},
                new Path[]{run.resolve("downRight.png"), convertedSpriteFolder.resolve("run-downright.png")},
                new Path[]{run.resolve("runDown.png"), convertedSpriteFolder.resolve("run-down.png")},
                new Path[]{run.resolve("runLeft.png"), convertedSpriteFolder.resolve("run-left.png")},
                new Path[]{run.resolve("runRight.png"), convertedSpriteFolder.resolve("run-right.png")},
                new Path[]{run.resolve("runUp.png"), convertedSpriteFolder.resolve("run-up.png")},
                new Path[]{run.resolve("upLeft.png"), convertedSpriteFolder.resolve("run-upleft.png")},
                new Path[]{run.resolve("upRight.png"), convertedSpriteFolder.resolve("run-upright.png")},

                // Walk folder
                new Path[]{walk.resolve("walkDown.png"), convertedSpriteFolder.resolve("walk-down.png")},
                new Path[]{walk.resolve("walkLeft.png"), convertedSpriteFolder.resolve("walk-left.png")},
                new Path[]{walk.resolve("walkRight.png"), convertedSpriteFolder.resolve("walk-right.png")},
                new Path[]{walk.resolve("walkUp.png"), convertedSpriteFolder.resolve("walk-up.png")},

                // Emote and idle sprites converted to other files
                new Path[]{spriteSheetFolder.resolve(emoteSpriteChoice), convertedSpriteFolder.resolve("emote.png")},
                new Path[]{spriteSheetFolder.resolve(patSpriteChoice), convertedSpriteFolder.resolve("pat.png")},
                new Path[]{spriteSheetFolder.resolve(pokeSprite), convertedSpriteFolder.resolve("poke.png")},

                // Sounds folder
                new Path[]{soundFolder.resolve("emote.wav"), convertedSoundFolder.resolve("emote.wav")},
                new Path[]{soundFolder.resolve("grab.wav"), convertedSoundFolder.resolve("grab.wav")},
                new Path[]{soundFolder.resolve("hover.wav"), convertedSoundFolder.resolve("hover.wav")},
                new Path[]{soundFolder.resolve("intro.wav"), convertedSoundFolder.resolve("intro.wav")},
                new Path[]{soundFolder.resolve("outro.wav"), convertedSoundFolder.resolve("outro.wav")},
                new Path[]{soundFolder.resolve("pat.wav"), convertedSoundFolder.resolve("pat.wav")},
                new Path[]{soundFolder.resolve("poke.wav"), convertedSoundFolder.resolve("poke.wav")},
                new Path[]{soundFolder.resolve("sleep.wav"), convertedSoundFolder.resolve("sleep.wav")},
                new Path[]{soundFolder.resolve(walkSound), convertedSoundFolder.resolve("walk.wav")},

                // Emote and other sounds (these sometimes don't have an original sound to copy and need placeholders)
                new Path[]{soundFolder.resolve(emoteSoundChoice), convertedSoundFolder.resolve("emote.wav")},
                new Path[]{soundFolder.resolve("emote2.wav"), convertedSoundFolder.resolve("poke.wav")},
                new Path[]{soundFolder.resolve(patSound), convertedSoundFolder.resolve("pat.wav")}
        );

        // Perform all copies
        for (Path[] pair : copies) {
            try {
                copyFile(pair[0], pair[1]);
            } catch (IOException e) {
                ioExceptionPrompt("Failed copying from " + pair[0] + " to " + pair[1], e);
            }
        }

        // Read config.txt
        Map<String, Integer> values = new HashMap<>();

        try (Stream<String> lines = Files.lines(originalConfigPath)) {
            lines.forEach(line -> {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("//")) return;

                // Skip SCALE
                if (line.startsWith("SCALE")) return;

                // Expect KEY=value
                String[] parts = line.split("=");
                if (parts.length != 2) return;

                String key = parts[0].trim();
                String val = parts[1].trim();

                try {
                    values.put(key, Integer.parseInt(val));
                } catch (NumberFormatException ignored) {
                    // Skip non-integer values
                }
            });
        } catch (IOException e) {
            ioExceptionPrompt("Failed to read config.txt", e);
        }

        // Sync choices/sprites with the equivalent key for proper frame count
        String emoteKey = convertSpriteToKey(emoteSpriteChoice);
        String patKey = convertSpriteToKey(patSpriteChoice);
        String pokeKey = convertSpriteToKey(pokeSprite);
        String introKey = convertSpriteToKey(introSprite);
        String outroKey = convertSpriteToKey(outroSprite);

        // Calculate emote duration for emote config
        Path soundFile = soundFolder.resolve(emoteSoundChoice);
        File file = soundFile.toFile();
        AudioInputStream ais = null;
        int durationMs;
        try {
            ais = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException | IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    """
                    Could not find emote sound file/could not read.
                    This could either be caused by selecting the wrong sound folder,
                    or if a Gremlin doesn't have an emote sound (this is common in
                    less popular gremlins such as Durandal who have incomplete
                    sprites and sounds)
                    
                    Proceeding without emote sound
                    """,
                    "Conversion Warning",
                    JOptionPane.WARNING_MESSAGE
            );
        }
        AudioFormat format;
        if (ais != null) {
            format = ais.getFormat();

            long frames = ais.getFrameLength();
            float frameRate = format.getFrameRate();

            durationMs = (int) Math.ceil((frames / frameRate) * 1000);
        } else {
            durationMs = 0;
        }

        // Create File contents
        // sprite-map.json
        List<String> spriteJsonLines = new ArrayList<>();

        spriteJsonLines.add("    \"FrameRate\": 60");
        spriteJsonLines.add("    \"SpriteColumn\": " + get(values, "COLUMN"));
        spriteJsonLines.add("    \"FrameHeight\": " + get(values, "HEIGHT"));
        spriteJsonLines.add("    \"FrameWidth\": " + get(values, "WIDTH"));
        spriteJsonLines.add("    \"TopHotspotHeight\": 175");
        spriteJsonLines.add("    \"TopHotspotWidth\": 150");
        spriteJsonLines.add("    \"SideHotspotHeight\": 0");
        spriteJsonLines.add("    \"SideHotspotWidth\": 0");
        spriteJsonLines.add("    \"HasReloadAnimation\": false");

        List<AssetEntry> spriteEntryList = List.of(
                new AssetEntry("Idle", "idle.png"),
                new AssetEntry("Hover", "hover.png"),
                new AssetEntry("Sleep", "sleep.png"),
                new AssetEntry("Intro", "intro.png"),
                new AssetEntry("Outro", "outro.png"),
                new AssetEntry("Grab", "grab.png"),
                new AssetEntry("Up", "run-up.png"),
                new AssetEntry("Down", "run-down.png"),
                new AssetEntry("Left", "run-left.png"),
                new AssetEntry("Right", "run-right.png"),
                new AssetEntry("UpLeft", "run-upleft.png"),
                new AssetEntry("UpRight", "run-upright.png"),
                new AssetEntry("DownLeft", "run-downleft.png"),
                new AssetEntry("DownRight", "run-downright.png"),
                new AssetEntry("WalkIdle", "walk-idle.png"),
                new AssetEntry("Poke", "poke.png"),
                new AssetEntry("Pat", "pat.png"),
                new AssetEntry("LeftAction", "left-action.png"),
                new AssetEntry("RightAction", "right-action.png"),
                new AssetEntry("Reload", "reload.png"),
                new AssetEntry("Emote", "emote.png")
        );

        // If the file corresponding to the sprite exists, it shows the filename as normal
        // Else shows the filename for idle.png,
        // except if it's LeftAction, RightAction or Reload, then it shows none
        // This fixes issues with low-sprite gremlins from being stuck and repeating a sprite
        Set<String> skip = Set.of("LeftAction", "RightAction", "Reload");

        for (AssetEntry entry : spriteEntryList) {
            Path currentSpriteFile = convertedSpriteFolder.resolve(entry.fileName());

            boolean exists = Files.exists(currentSpriteFile);

            String value = exists
                    ? entry.fileName()
                    : (skip.contains(entry.key()) ? "" : "idle.png");

            spriteJsonLines.add("    \"" + entry.key() + "\": \"" + value + "\"");
        }

        String spriteSheetFile =
                "{\n" +
                        String.join(",\n", spriteJsonLines) +
                        "\n}";

        // frame-count.json
        // The idle sprite replaces missing sprites (except LeftAction, RightAction and Reload)
        // Therefore, this makes the frame count sync by using the idle frame count for them
        for (String key : values.keySet()) {
            if (get(values, key) == 0 && !skip.contains(key)) {
                values.put(key, get(values, "IDLE"));
            }
        }

        String frameCountFile = "{\n" +
                "    \"Idle\": " + get(values, "IDLE") + ",\n" +
                "    \"Hover\": " + get(values, "HOVER") + ",\n" +
                "    \"Sleep\": " + get(values, "SLEEP") + ",\n" +
                "    \"Intro\": " + get(values, introKey) + ",\n" +
                "    \"Outro\": " + get(values, outroKey) + ",\n" +
                "    \"Grab\": " + get(values, "GRAB") + ",\n" +
                "    \"Up\": " + get(values, "RUNUP") + ",\n" +
                "    \"Down\": " + get(values, "RUNDOWN") + ",\n" +
                "    \"Left\": " + get(values, "RUNLEFT") + ",\n" +
                "    \"Right\": " + get(values, "RUNRIGHT") + ",\n" +
                "    \"UpLeft\": " + get(values, "UPLEFT") + ",\n" +
                "    \"UpRight\": " + get(values, "UPRIGHT") + ",\n" +
                "    \"DownLeft\": " + get(values, "DOWNLEFT") + ",\n" +
                "    \"DownRight\": " + get(values, "DOWNRIGHT") + ",\n" +
                "    \"WalkIdle\": " + get(values, "RUNIDLE") + ",\n" +
                "    \"Poke\": " + get(values, pokeKey) + ",\n" +
                "    \"Pat\": " + get(values, patKey) + ",\n" +
                "    \"LeftAction\": 0,\n" +
                "    \"RightAction\": 0,\n" +
                "    \"Reload\": 0,\n" +
                "    \"Emote\": " + get(values, emoteKey) + "\n" +
                "}";

        // emote-config.json
        String emoteConfigFile =
                "{\n" +
                        "    \"AnnoyEmote\": true,\n" +
                        "    \"MinEmoteTriggerMinutes\": 5,\n" +
                        "    \"MaxEmoteTriggerMinutes\": 15,\n" +
                        "    \"EmoteDuration\": " + durationMs + "\n" +
                        "}";

        // sfx-map.json
        List<String> soundJsonLines = new ArrayList<>();

        List<AssetEntry> soundEntryList = List.of(
                new AssetEntry("Hover", "hover.wav"),
                new AssetEntry("Intro", "intro.wav"),
                new AssetEntry("Outro", "outro.wav"),
                new AssetEntry("Grab", "grab.wav"),
                new AssetEntry("Walk", "walk.wav"),
                new AssetEntry("Poke", "poke.wav"),
                new AssetEntry("Pat", "pat.wav"),
                new AssetEntry("LeftAction", "left-action.wav"),
                new AssetEntry("RightAction", "right-action.wav"),
                new AssetEntry("Reload", "reload.wav"),
                new AssetEntry("Emote", "emote.wav")
        );

        // Shows the corresponding filename if it exists, else shows "" (none)
        for (AssetEntry entry : soundEntryList) {
            Path currentSoundFile = convertedSoundFolder.resolve(entry.fileName());
            String value = Files.exists(currentSoundFile) ? entry.fileName() : "";
            soundJsonLines.add("    \"" + entry.key() + "\": \"" + value + "\"");
        }

        String sfxMapFile =
                "{\n" +
                        String.join(",\n", soundJsonLines) +
                        "\n}";

        // Write files
        try {
            Files.writeString(frameCountPath, frameCountFile);
            Files.writeString(spriteMapPath, spriteSheetFile);
            Files.writeString(emoteConfigPath, emoteConfigFile);
            Files.writeString(sfxMapPath, sfxMapFile);
        } catch (IOException e) {
            ioExceptionPrompt("Failed to write frame-count/sprite-sheet/emote-config/sfx-map file", e);
        }

        // Copies gremlin to .config directory if present
        boolean gremlinsDirExists = Files.isDirectory(gremlinsDir);

        if (gremlinsDirExists) {
            try {
                copyFolder(gremlinFolder, gremlinsDir.resolve(normalized));
            } catch (IOException e) {
                ioExceptionPrompt("Failed to copy converted files to .config directory (even though it exists)", e);
            }

            JOptionPane.showMessageDialog(
                    null,
                    """
                            Converted files successfully!
                            These have automatically been copied to `.config/linux-desktop-gremlin`
                            and **should now be available in the Gremlin Picker!**
                            
                            The converted gremlin is in:
                            `~/ConvertedGremlins`
                            """,
                    "Conversion finished!",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    """
                            Converted files successfully!
                            The converted gremlin is in:
                            `~/ConvertedGremlins`
                            """,
                    "Conversion finished!",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        scanner.close();
        }
    }
    static void copyFile(Path from, Path to) throws IOException {
        if (!Files.exists(from)) {
            return; // Skip missing files
        }

        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyFolder(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path destination = target.resolve(relative);

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    ioExceptionPrompt("Failed to copy folder at:" + path, e);
                }
            });
        }
    }

    private static String chooseSprite(String message, String title, String[] Options) {
        String selectedDisplay = (String) JOptionPane.showInputDialog(
                null,
                message,
                title,
                JOptionPane.QUESTION_MESSAGE,
                null,
                Options,
                Options[0]
        );

        if (selectedDisplay == null) {
            userCancel();
        }

        return selectedDisplay;
    }

    private static int get(Map<String, Integer> map, String key) {
        return map.getOrDefault(key, 0);
    }

    private static String convertSpriteToKey(String choice) {
        return switch (choice) {
            case "Emotes/emote1.png" -> "EMOTE1";
            case "Emotes/emote2.png" -> "EMOTE2";
            case "Emotes/emote3.png" -> "EMOTE3";
            case "Emotes/emote4.png" -> "EMOTE4";
            case "Actions/idle.png"  -> "IDLE";
            case "Actions/click.png"  -> "CLICK";
            case "intro.png"  -> "INTRO";
            case "outro.png"  -> "OUTRO";
            case "grab.png"  -> "GRAB";
            default -> null;
        };
    }

    private record AssetEntry(String key, String fileName) {}

    private static void userCancel() {
        // Happens if user clicks cancel
        JOptionPane.showMessageDialog(
                null,
                "Conversion cancelled by user",
                "Conversion Cancelled",
                JOptionPane.INFORMATION_MESSAGE
        );

        throw new CancellationException("User cancelled conversion");
    }

    private static void ioExceptionPrompt(String message, IOException e) {
        // Happens if user clicks cancel
        JOptionPane.showMessageDialog(
                null,
                "An unexpected file operation error occurred. This shouldn’t happen during normal use. Please report this issue.",
                "Conversion Failed",
                JOptionPane.ERROR_MESSAGE
        );

        throw new RuntimeException(
                "\n\nUnexpected IOException: " + message +
                        "\nCause: " + e.getMessage() + "\n\n",
                e
        );
    }
}
