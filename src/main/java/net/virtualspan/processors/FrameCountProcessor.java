package net.virtualspan.processors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class FrameCountProcessor {
    public static String process(
            Path convertedSpriteFolder,
            String emoteSpriteChoice,
            String patSpriteChoice,
            String pokeSprite,
            String introSprite,
            String outroSprite,
            String normalised,
            Set<String> skip,
            Map<String, Integer> values) {
        // Sync choices/sprites with the equivalent key for proper frame count
        String emoteKey = convertSpriteToKey(emoteSpriteChoice);
        String patKey = convertSpriteToKey(patSpriteChoice);
        String pokeKey = convertSpriteToKey(pokeSprite);
        String introKey = convertSpriteToKey(introSprite);
        String outroKey = convertSpriteToKey(outroSprite);

        // frame-count.json
        // The idle sprite replaces missing sprites (except LeftAction, RightAction and Reload)
        // Therefore, this makes the frame count sync by using the idle frame count for them
        for (String key : values.keySet()) {
            if (get(values, key) == 0 && !skip.contains(key)) {
                values.put(key, get(values, "IDLE"));
            }
        }

        // Movement sprites fall back to run-right.png and/or run-left.png if it exists, so this syncs frame count for that
        Set<String> rightMovementKeys = Set.of(
                "RUNUP", "RUNRIGHT", "UPRIGHT", "DOWNRIGHT"
        );

        Set<String> leftMovementKeys = Set.of(
                "RUNDOWN", "RUNLEFT", "UPLEFT", "DOWNLEFT"
        );

        boolean hasRunRight = Files.exists(convertedSpriteFolder.resolve("run-right.png"));
        boolean hasRunLeft  = Files.exists(convertedSpriteFolder.resolve("run-left.png"));

        // If run-right exists, apply RUNRIGHT frames
        if (hasRunRight) {
            int runRightFrames = get(values, "RUNRIGHT");

            // Always update right movement keys
            for (String key : rightMovementKeys) {
                values.put(key, runRightFrames);
            }

            // If run-left does NOT exist, left keys also fall back to run-right
            if (!hasRunLeft) {
                for (String key : leftMovementKeys) {
                    values.put(key, runRightFrames);
                }
            }
        }

        // If run-left exists, apply RUNLEFT frames
        if (hasRunLeft) {
            int runLeftFrames = get(values, "RUNLEFT");

            // Always update left movement keys
            for (String key : leftMovementKeys) {
                values.put(key, runLeftFrames);
            }

            // If run-right does NOT exist, right keys also fall back to run-left
            if (!hasRunRight) {
                for (String key : rightMovementKeys) {
                    values.put(key, runLeftFrames);
                }
            }
        }

        // Hardcode frame count fix for Gold Ship
        if (normalised.equals("goldship")) {
            values.put("HOVER", 25);
            values.put("SLEEP", 50);
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
        return frameCountFile;
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

    private static int get(Map<String, Integer> map, String key) {
        return map.getOrDefault(key, 0);
    }
}
