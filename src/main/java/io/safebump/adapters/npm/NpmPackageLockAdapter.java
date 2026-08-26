package io.safebump.adapters.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.safebump.core.adapter.DependencySourceAdapter;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Builds a resolved npm graph from package-lock v2 or v3 metadata. */
public final class NpmPackageLockAdapter implements DependencySourceAdapter {

    private static final Set<Integer> SUPPORTED_LOCKFILE_VERSIONS = Set.of(2, 3);
    private final ObjectMapper objectMapper;

    public NpmPackageLockAdapter() {
        this(new ObjectMapper());
    }

    NpmPackageLockAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public DependencySnapshot load(Path source) throws DependencySourceException {
        Objects.requireNonNull(source, "source");
        Path directory = source.toAbsolutePath().normalize();
        Path lockfile = Files.isDirectory(directory)
                ? selectLockfile(directory)
                : directory;
        JsonNode root = read(lockfile);
        int lockfileVersion = root.path("lockfileVersion").asInt(-1);
        if (!SUPPORTED_LOCKFILE_VERSIONS.contains(lockfileVersion)) {
            throw new DependencySourceException(
                    "Unsupported npm lockfileVersion " + lockfileVersion + " in " + lockfile
                            + "; SafeBump supports versions 2 and 3");
        }
        JsonNode packagesNode = root.get("packages");
        if (packagesNode == null || !packagesNode.isObject()) {
            throw new DependencySourceException(
                    "npm lockfile has no object-valued packages map: " + lockfile);
        }
        JsonNode rootNode = packagesNode.get("");
        if (rootNode == null || !rootNode.isObject()) {
            throw new DependencySourceException(
                    "npm lockfile packages map has no root entry: " + lockfile);
        }

        String rootName = requiredText(rootNode, "name", lockfile);
        String rootVersion = optionalText(rootNode, "version", "0.0.0");
        PackageVersion rootPackage = new PackageVersion(rootName, rootVersion);
        Map<String, PackageEntry> entries = parseEntries(packagesNode, rootPackage, lockfile);
        DependencyGraph graph = new DependencyGraph();
        Map<String, PackageMetadata> metadata = new TreeMap<>();
        for (PackageEntry entry : entries.values()) {
            graph.addPackage(entry.packageVersion());
            metadata.putIfAbsent(
                    entry.packageVersion().toString(),
                    new PackageMetadata(
                            entry.packageVersion(), entry.kind(), entry.source()));
        }

        for (PackageEntry entry : entries.values()) {
            for (String dependencyName : entry.dependencies()) {
                PackageEntry dependency = resolve(entries, entry.path(), dependencyName);
                if (dependency != null) {
                    graph.addDependency(entry.packageVersion(), dependency.packageVersion());
                }
            }
        }
        return new DependencySnapshot(rootPackage, graph, metadata);
    }

    private Map<String, PackageEntry> parseEntries(
            JsonNode packagesNode,
            PackageVersion rootPackage,
            Path lockfile) throws DependencySourceException {
        Map<String, PackageEntry> entries = new LinkedHashMap<>();
        JsonNode rootNode = packagesNode.get("");
        Set<String> directDependencies = new TreeSet<>(
                fieldNames(rootNode.get("dependencies")));
        directDependencies.addAll(fieldNames(rootNode.get("optionalDependencies")));
        directDependencies.addAll(fieldNames(rootNode.get("peerDependencies")));
        Set<String> devDependencies = fieldNames(rootNode.get("devDependencies"));
        Set<String> rootDependencies = new TreeSet<>(directDependencies);
        rootDependencies.addAll(devDependencies);
        entries.put("", new PackageEntry(
                "", rootPackage, DependencyKind.ROOT, "root",
                Set.copyOf(rootDependencies)));

        for (Map.Entry<String, JsonNode> field : packagesNode.properties()) {
            String packagePath = field.getKey();
            if (packagePath.isEmpty()) {
                continue;
            }
            JsonNode packageNode = field.getValue();
            if (!packageNode.isObject() || packageNode.path("link").asBoolean(false)) {
                continue;
            }
            String name = packageName(packagePath);
            String version = requiredText(packageNode, "version", lockfile);
            DependencyKind kind = topLevel(packagePath) && directDependencies.contains(name)
                    ? DependencyKind.DIRECT
                    : topLevel(packagePath) && devDependencies.contains(name)
                            ? DependencyKind.DEV
                            : DependencyKind.TRANSITIVE;
            entries.put(packagePath, new PackageEntry(
                    packagePath,
                    new PackageVersion(name, version),
                    kind,
                    source(packageNode),
                    dependencyNames(packageNode)));
        }
        return entries;
    }

    private static PackageEntry resolve(
            Map<String, PackageEntry> entries,
            String fromPath,
            String dependencyName) {
        String current = fromPath;
        while (true) {
            String candidate = current.isEmpty()
                    ? "node_modules/" + dependencyName
                    : current + "/node_modules/" + dependencyName;
            PackageEntry resolved = entries.get(candidate);
            if (resolved != null) {
                return resolved;
            }
            if (current.isEmpty()) {
                return null;
            }
            int parentBoundary = current.lastIndexOf("/node_modules/");
            current = parentBoundary < 0 ? "" : current.substring(0, parentBoundary);
        }
    }

    private static Set<String> dependencyNames(JsonNode packageNode) {
        Set<String> names = new TreeSet<>();
        names.addAll(fieldNames(packageNode.get("dependencies")));
        names.addAll(fieldNames(packageNode.get("optionalDependencies")));
        names.addAll(fieldNames(packageNode.get("peerDependencies")));
        return Set.copyOf(names);
    }

    private static Set<String> fieldNames(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Set.of();
        }
        Set<String> names = new TreeSet<>();
        node.properties().forEach(entry -> names.add(entry.getKey()));
        return Set.copyOf(names);
    }

    private static String packageName(String packagePath) {
        int boundary = packagePath.lastIndexOf("node_modules/");
        return packagePath.substring(boundary + "node_modules/".length());
    }

    private static boolean topLevel(String packagePath) {
        return packagePath.indexOf("/node_modules/") < 0;
    }

    private static String source(JsonNode packageNode) {
        if (packageNode.path("inBundle").asBoolean(false)) {
            return "bundled";
        }
        String resolved = packageNode.path("resolved").asText("");
        if (resolved.startsWith("git+") || resolved.startsWith("git:")) {
            return "git";
        }
        if (resolved.startsWith("file:")) {
            return "file";
        }
        return "npm";
    }

    private JsonNode read(Path lockfile) throws DependencySourceException {
        if (!Files.isRegularFile(lockfile)) {
            throw new DependencySourceException("No npm lockfile found: " + lockfile);
        }
        try {
            return objectMapper.readTree(lockfile.toFile());
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not parse npm lockfile " + lockfile + ": "
                            + exception.getMessage(), exception);
        }
    }

    private static Path selectLockfile(Path directory) throws DependencySourceException {
        List<Path> candidates = new ArrayList<>();
        candidates.add(directory.resolve("package-lock.json"));
        candidates.add(directory.resolve("npm-shrinkwrap.json"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new DependencySourceException(
                        "No package-lock.json or npm-shrinkwrap.json found in npm project: "
                                + directory));
    }

    private static String requiredText(JsonNode node, String field, Path source)
            throws DependencySourceException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new DependencySourceException(
                    "Missing text field '" + field + "' in " + source);
        }
        return value.asText().trim();
    }

    private static String optionalText(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank()
                ? value.asText().trim()
                : fallback;
    }

    private record PackageEntry(
            String path,
            PackageVersion packageVersion,
            DependencyKind kind,
            String source,
            Set<String> dependencies) {
    }
}
