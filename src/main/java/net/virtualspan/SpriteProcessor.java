package net.virtualspan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.virtualspan.FileUtils.copyFile;
import static net.virtualspan.FileUtils.ioExceptionPrompt;

public class SpriteProcessor {
    public static String process(Path spriteSheetFolder, Path convertedSpriteFolder, Path actions,
                               Path run, String introSprite, String outroSprite, String emoteSpriteChoice,
                               String patSpriteChoice, String pokeSprite, Set<String> skip, Map<String, Integer> values) {
        // Copies and renames hover.png or idle.png to intro.png and outro.png as a placeholder if they aren't present
        // This fixes issues with starting/closing the Gremlin and gets overridden if they are present

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
        } catch (
                IOException e) {
            ioExceptionPrompt("Failed to copy and rename to intro/outro.png placeholder files", e);
        }

        // List of copy operations
        List<Path[]> spriteCopies = List.of(
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

                // Emote and idle sprites converted to other files
                new Path[]{spriteSheetFolder.resolve(emoteSpriteChoice), convertedSpriteFolder.resolve("emote.png")},
                new Path[]{spriteSheetFolder.resolve(patSpriteChoice), convertedSpriteFolder.resolve("pat.png")},
                new Path[]{spriteSheetFolder.resolve(pokeSprite), convertedSpriteFolder.resolve("poke.png")}
                );

        // Perform all sprite file copies
        for (Path[] pair : spriteCopies) {
            try {
                copyFile(pair[0], pair[1]);
            } catch (IOException e) {
                ioExceptionPrompt("Failed copying from " + pair[0] + " to " + pair[1], e);
            }
        }

        // sprite-map.json
        List<String> spriteJsonLines = new ArrayList<>();

        spriteJsonLines.add("    \"FrameRate\": 60");
        spriteJsonLines.add("    \"SpriteColumn\": " + values.get("COLUMN"));
        spriteJsonLines.add("    \"FrameHeight\": " + values.get("HEIGHT"));
        spriteJsonLines.add("    \"FrameWidth\": " + values.get("WIDTH"));
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
        // Also uses run-right and/or run-left sprites (depending on what's available)
        // instead of idle.png if movement sprites don't exist
        Set<String> rightMovementSprites = Set.of(
                "Up", "Right", "UpRight", "DownRight"
        );

        Set<String> leftMovementSprites = Set.of(
                "Down", "Left", "UpLeft", "DownLeft"
        );

        String runRightIfExists;
        String runLeftIfExists;

        if (Files.exists(convertedSpriteFolder.resolve("run-right.png"))) {
            runRightIfExists = "run-right.png";
        } else {
            runRightIfExists = "idle.png";
        }

        if (Files.exists(convertedSpriteFolder.resolve("run-left.png"))) {
            runLeftIfExists = "run-left.png";
        } else {
            runLeftIfExists = "idle.png";
        }

        for (AssetEntry entry : spriteEntryList) {
            Path currentSpriteFile = convertedSpriteFolder.resolve(entry.fileName());

            boolean exists = Files.exists(currentSpriteFile);

            String value = exists
                    ? entry.fileName()
                    : (skip.contains(entry.key())
                    ? ""
                    : rightMovementSprites.contains(entry.key())
                    ? runRightIfExists
                    : leftMovementSprites.contains(entry.key())
                    ? runLeftIfExists
                    : "idle.png"
            );

            spriteJsonLines.add("    \"" + entry.key() + "\": \"" + value + "\"");
        }

        String spriteSheetFile =
                "{\n" +
                        String.join(",\n", spriteJsonLines) +
                        "\n}";

        return spriteSheetFile;
    }
}
