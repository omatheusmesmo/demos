package dev.omatheusmesmo.demos.invoicing.audit;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AuditLogTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void appendsEveryEventOnItsOwnLine() throws Exception {
        File file = new File(folder.getRoot(), "audit.log");
        new AuditLog(file.getPath()).record("CREATED A");
        new AuditLog(file.getPath()).record("CREATED B");

        assertEquals(Arrays.asList("CREATED A", "CREATED B"), Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }
}
