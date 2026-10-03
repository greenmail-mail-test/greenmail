package com.icegreen.greenmail.server;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Base GreenMail test (no pre-configured GreenMail server)
 */
public class GreenMailBaseTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testCustomMailSessionProperties() {
        GreenMail greenMail = new GreenMail(new ServerSetup[]{
            ServerSetupTest.SMTP,
            ServerSetupTest.IMAP.mailSessionProperty("a.key", "a.value")});
        assertThat(greenMail.getImap().createSession().getProperties()).contains(entry("a.key", "a.value"));
        assertThat(greenMail.getSmtp().createSession().getProperties()).doesNotContain(entry("a.key", "a.value"));
    }

    @Test
    public void testUnixStyleHiddenPathsAreIgnored() throws IOException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP);
        final Path baseDirectory = temporaryFolder.newFolder().toPath();
        final Path hiddenFile = Files.createFile(baseDirectory.resolve(".hidden.eml"));
        final Path hiddenDirectory = Files.createDirectory(baseDirectory.resolve(".hidden"));
        final Path fileInHiddenDirectory = Files.createFile(hiddenDirectory.resolve("message.eml"));
        final Path visibleFile = Files.createFile(baseDirectory.resolve("visible.eml"));

        assertThat(greenMail.isHiddenOrInHiddenDir(baseDirectory, hiddenFile)).isTrue();
        assertThat(greenMail.isHiddenOrInHiddenDir(baseDirectory, fileInHiddenDirectory)).isTrue();
        assertThat(greenMail.isHiddenOrInHiddenDir(baseDirectory, visibleFile)).isFalse();
    }
}
