package io.safebump.core.ecosystem;

import io.safebump.core.adapter.DependencySourceException;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/** Loads ecosystem providers from the classpath and an optional JAR directory. */
public final class PluginProviderLoader {

    public List<DependencyEcosystemProvider> load(Path pluginDirectory)
            throws DependencySourceException {
        List<DependencyEcosystemProvider> providers = new ArrayList<>();
        loadFrom(ServiceLoader.load(DependencyEcosystemProvider.class), providers);
        if (pluginDirectory == null) {
            return List.copyOf(providers);
        }

        Path normalized = pluginDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new DependencySourceException(
                    "Plugin directory does not exist: " + normalized);
        }
        try {
            URL[] jars;
            try (var paths = Files.list(normalized)) {
                jars = paths.filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .sorted(Comparator.comparing(Path::toString))
                        .map(PluginProviderLoader::toUrl)
                        .toArray(URL[]::new);
            }
            URLClassLoader classLoader = new URLClassLoader(
                    jars, DependencyEcosystemProvider.class.getClassLoader());
            ServiceLoader.load(DependencyEcosystemProvider.class, classLoader).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(provider -> provider.getClass().getClassLoader() == classLoader)
                    .forEach(providers::add);
            return List.copyOf(providers);
        } catch (IOException | ServiceConfigurationError exception) {
            throw new DependencySourceException(
                    "Could not load ecosystem plugins from " + normalized + ": "
                            + exception.getMessage(), exception);
        }
    }

    private static void loadFrom(
            ServiceLoader<DependencyEcosystemProvider> loader,
            List<DependencyEcosystemProvider> target) {
        loader.forEach(target::add);
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalArgumentException("Invalid plugin path: " + path, exception);
        }
    }
}
