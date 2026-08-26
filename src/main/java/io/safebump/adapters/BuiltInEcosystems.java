package io.safebump.adapters;

import io.safebump.adapters.dart.DartEcosystemProvider;
import io.safebump.adapters.gradle.GradleEcosystemProvider;
import io.safebump.adapters.maven.MavenEcosystemProvider;
import io.safebump.adapters.npm.NpmEcosystemProvider;
import io.safebump.adapters.python.PythonEcosystemProvider;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.ecosystem.DependencyEcosystemProvider;
import io.safebump.core.ecosystem.EcosystemRegistry;
import io.safebump.core.ecosystem.PluginProviderLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Creates registries containing SafeBump's built-in and discovered providers. */
public final class BuiltInEcosystems {

    private BuiltInEcosystems() {
    }

    public static EcosystemRegistry create(Path pluginDirectory)
            throws DependencySourceException {
        List<DependencyEcosystemProvider> providers = new ArrayList<>(List.of(
                new DartEcosystemProvider(),
                new NpmEcosystemProvider(),
                new PythonEcosystemProvider(),
                new MavenEcosystemProvider(),
                new GradleEcosystemProvider()));
        providers.addAll(new PluginProviderLoader().load(pluginDirectory));
        return new EcosystemRegistry(providers);
    }
}
