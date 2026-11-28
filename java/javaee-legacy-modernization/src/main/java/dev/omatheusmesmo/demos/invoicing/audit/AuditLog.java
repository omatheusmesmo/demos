package dev.omatheusmesmo.demos.invoicing.audit;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class AuditLog {

    public static final String FILE_PROPERTY = "invoicing.audit.file";

    private final Writer writer;

    public AuditLog() throws IOException {
        this(System.getProperty(FILE_PROPERTY, System.getProperty("java.io.tmpdir") + "/invoicing-audit.log"));
    }

    public AuditLog(String path) throws IOException {
        this.writer = new FileWriter(path, true);
    }

    public void record(String event) throws IOException {
        writer.write(event);
        writer.write(System.lineSeparator());
        writer.flush();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            writer.close();
        } finally {
            super.finalize();
        }
    }
}
