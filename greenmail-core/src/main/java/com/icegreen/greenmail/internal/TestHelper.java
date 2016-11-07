package com.icegreen.greenmail.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.icegreen.greenmail.filestore.MBoxFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for unit tests. Contains only static methods which should only be called from tests... these methods are not
 * production ready (e.g. regarding exception handling) and are fine to be used inside tests, but not inside the real
 * production code!
 *
 * Created by saladin on 11/4/16.
 */
public class TestHelper {
    private static final Logger log = LoggerFactory.getLogger(MBoxFileStore.class);

    public static final String getRandomDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "greenmail_" + Long.toString(System.nanoTime())).toString();
    }

    public static void deleteDirectoryWithContentAndIgnoreExceptions(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException io) {
            // Ignore inner exceptions... Let's just leave the directory as it is, this is unfortunate but now
            log.warn("Exception happened while deleting directory '" + directory.toAbsolutePath().toString() + "'. But continue"
                    + " anyhow.", io);
        }

    }

}
