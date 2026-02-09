package net.virtualspan;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;

import static net.virtualspan.FileUtils.copyFolder;
import static net.virtualspan.FileUtils.ioExceptionPrompt;

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
        String normalised = folderName.toLowerCase().replace(" ", "-");

        // Export Folder Paths
        Path exportFolder = Path.of(System.getProperty("user.home"), "ConvertedGremlins");
        Path gremlinFolder = exportFolder.resolve(normalised);
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
        // If config file isn't present, stops converter
        JOptionPane.showMessageDialog(
                null,
                """
                        config.txt was not found.
                        Please make sure you are selecting the correct sprite folder.
                        """,
                "Missing Config File",
                JOptionPane.ERROR_MESSAGE
        );

        throw new RuntimeException("config.txt not found");
    } else if (!Files.exists(spriteSheetFolder.resolve("Actions/idle.png"))) {
        // If idle.png isn't present, stops converter
        JOptionPane.showMessageDialog(
                null,
                """
                        idle.png was not found in the sprite folder.
                        The Gremlin you have chosen is most likely incompatible.
                        """,
                "Missing idle.png File",
                JOptionPane.ERROR_MESSAGE
        );

        throw new RuntimeException("Actions/idle.png not found");
    } else if (!Files.exists(soundFolder.resolve("intro.wav"))) {
        // If intro.wav is not present (which is in every sound folder), stops converter
        JOptionPane.showMessageDialog(
                null,
                """
                        Sound files are not present!.
                        You selected the wrong sound folder.
                        """,
                "Incorrect Sound folder selected",
                JOptionPane.ERROR_MESSAGE
        );

        throw new RuntimeException("intro.wav was not found. An incorrect sound folder was most likely selected");
    } else {
        Scanner scanner = new Scanner(System.in);

        // Sprite Options for the dropdown menu (only shows options that exist)
        List<String> spriteOptionsBuilder = new ArrayList<>();
        spriteOptionsBuilder.add("default");
        String[] spriteDirs = {
                "Emotes/emote1.png",
                "Emotes/emote2.png",
                "Emotes/emote3.png",
                "Emotes/emote4.png",
                "Actions/click.png"
        };
        for (String option : spriteDirs) {
            Path filePath = spriteSheetFolder.resolve(option);
            if (Files.exists(filePath)) {
                spriteOptionsBuilder.add(option);
            }
        }
        spriteOptionsBuilder.add("Actions/idle.png");
        String[] spriteOptions = spriteOptionsBuilder.toArray(new String[0]);

        // Sound Options for the dropdown menu
        List<String> soundOptionsBuilder = new ArrayList<>();
        soundOptionsBuilder.add("default");

        String[] soundFiles = {
                "emote1.wav",
                "emote2.wav",
                "emote3.wav",
                "emote4.wav"
        };

        for (String option : soundFiles) {
            Path filePath = soundFolder.resolve(option);
            if (Files.exists(filePath)) {
                soundOptionsBuilder.add(option);
            }
        }

        String[] soundOptions = soundOptionsBuilder.toArray(new String[0]);

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

        // Shared variables between classes
        String introSprite = "intro.png";
        String outroSprite = "outro.png";
        Set<String> skip = Set.of("LeftAction", "RightAction", "Reload");
        Map<String, Integer> values = new HashMap<>();

        // Return back Strings from classes
        String frameCountFile = FrameCountProcessor.process(originalConfigPath, convertedSpriteFolder, emoteSpriteChoice,
                patSpriteChoice, pokeSprite, introSprite, outroSprite, normalised, skip, values);

        String spriteSheetFile = SpriteProcessor.process(spriteSheetFolder, convertedSpriteFolder, actions,
                run, introSprite, outroSprite, emoteSpriteChoice,
                patSpriteChoice, pokeSprite, skip, values);

        SoundResult sound = SoundProcessor.process(soundFolder, convertedSoundFolder, walkSound, emoteSoundChoice, patSound);
        String emoteConfigFile = sound.emoteConfig();
        String sfxMapFile = sound.sfxMap();

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
                copyFolder(gremlinFolder, gremlinsDir.resolve(normalised));
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
}
