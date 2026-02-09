package net.virtualspan;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class FileUtils {
    public static void copyFile(Path from, Path to) throws IOException {
        if (!Files.exists(from)) {
            return; // Skip missing files
        }

        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFolder(Path source, Path target) throws IOException {
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

    public static void ioExceptionPrompt(String message, IOException e) {
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
