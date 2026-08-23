package io.safebump.adapters.dart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.safebump.core.adapter.DependencySourceException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Shared YAML reading and validation helpers for Dart project files. */
final class DartYamlSupport {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private DartYamlSupport() {
    }

    static JsonNode read(Path source, String fileType) throws DependencySourceException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(fileType, "fileType");

        try (Reader reader = Files.newBufferedReader(source)) {
            JsonNode document = YAML_MAPPER.readTree(reader);
            if (document == null || !document.isObject()) {
                throw new DependencySourceException(
                        "Invalid " + fileType + " in " + source
                                + ": document must be a YAML mapping");
            }
            return document;
        } catch (JsonProcessingException exception) {
            throw new DependencySourceException(
                    "Invalid " + fileType + " YAML in " + source + ": "
                            + exception.getOriginalMessage(),
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not read " + fileType + " from " + source + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    static String requiredText(JsonNode object, String fieldName, String context) {
        JsonNode value = object.get(fieldName);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(
                    context + "." + fieldName + " must be a non-blank scalar");
        }
        return value.textValue().trim();
    }
}
