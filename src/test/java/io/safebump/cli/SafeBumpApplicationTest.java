package io.safebump.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeBumpApplicationTest {

    @Test
    void displaysRootHelpAndListsAvailableCommands() {
        CommandLine commandLine = new CommandLine(new SafeBumpApplication());
        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("--help");

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Usage: safebump"));
        assertTrue(output.toString().contains("graph"));
        assertTrue(output.toString().contains("detect"));
        assertTrue(output.toString().contains("compare"));
        assertTrue(output.toString().contains("solve"));
        assertTrue(output.toString().contains("pr-report"));
    }

    @Test
    void displaysTheApplicationVersion() {
        CommandLine commandLine = new CommandLine(new SafeBumpApplication());
        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("--version");

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("SafeBump 0.1.0"));
    }
}
