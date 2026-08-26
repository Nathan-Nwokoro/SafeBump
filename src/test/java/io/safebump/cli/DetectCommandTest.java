package io.safebump.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectCommandTest {

    @TempDir
    Path projectDirectory;

    @Test
    void detectsNpmProjectFromItsLockfile() throws Exception {
        Files.writeString(projectDirectory.resolve("package-lock.json"), "{}");
        CommandLine commandLine = new CommandLine(new DetectCommand());
        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute(projectDirectory.toString());

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Detected ecosystem: npm (npm / Node.js)"));
    }

    @Test
    void reportsUnknownProjectMarkers() {
        CommandLine commandLine = new CommandLine(new DetectCommand());
        StringWriter error = new StringWriter();
        commandLine.setErr(new PrintWriter(error, true));

        int exitCode = commandLine.execute(projectDirectory.toString());

        assertEquals(2, exitCode);
        assertTrue(error.toString().contains("Could not detect"));
    }
}
