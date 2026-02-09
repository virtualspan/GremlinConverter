package net.virtualspan;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.virtualspan.FileUtils.copyFile;
import static net.virtualspan.FileUtils.ioExceptionPrompt;

public class SoundProcessor {
    public static SoundResult process(Path soundFolder, Path convertedSoundFolder, String walkSound,
                               String emoteSoundChoice, String patSound) {
        // List of copy operations
        List<Path[]> soundCopies = List.of(
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

        // Perform all sound file copies
        for (Path[] pair : soundCopies) {
            try {
                copyFile(pair[0], pair[1]);
            } catch (IOException e) {
                ioExceptionPrompt("Failed copying from " + pair[0] + " to " + pair[1], e);
            }
        }

        // Calculate emote duration for emote config
        Path soundFile = soundFolder.resolve(emoteSoundChoice);
        File file = soundFile.toFile();
        AudioInputStream ais = null;
        int durationMs;
        try {
            ais = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException | IOException _) {
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

        return new SoundResult(emoteConfigFile, sfxMapFile);
    }
}