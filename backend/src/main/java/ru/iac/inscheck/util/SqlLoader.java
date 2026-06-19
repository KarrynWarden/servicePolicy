package ru.iac.inscheck.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Загрузка SQL из отдельных файлов (правило постановки: «sql в отдельных файлах»).
 * Аналог strUtils.getSql из присланного примера бэка. Файлы лежат в
 * src/main/resources/sql/, путь указывается относительно этой папки.
 */
@Component
public class SqlLoader {

    private static final String BASE = "sql/";
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public String getSql(String relativePath) {
        return cache.computeIfAbsent(relativePath, this::read);
    }

    private String read(String relativePath) {
        ClassPathResource resource = new ClassPathResource(BASE + relativePath);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не найден SQL-файл: " + BASE + relativePath, e);
        }
    }
}
