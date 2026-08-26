package io.safebump.core.ecosystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginProviderLoaderTest {

    @Test
    void discoversProvidersDeclaredThroughJavaServiceLoader() throws Exception {
        var providers = new PluginProviderLoader().load(null);

        assertEquals(
                java.util.List.of("test-plugin"),
                providers.stream().map(DependencyEcosystemProvider::id).toList());
    }
}
