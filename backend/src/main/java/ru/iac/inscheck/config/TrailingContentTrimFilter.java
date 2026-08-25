package ru.iac.inscheck.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Отсекает всё, что идёт ПОСЛЕ закрывающего тега конверта (…&lt;/…Envelope&gt;) во входящем
 * запросе — комментарии, пробелы, мусор. Строгий XML-парсер на таком «хвосте» падает, и
 * ответ получается пустым; старый сервис такие запросы обрабатывал нормально.
 *
 * Трогаем только «хвост» после конверта — сам конверт остаётся как есть.
 */
public class TrailingContentTrimFilter extends OncePerRequestFilter {

    private static final String ENVELOPE_CLOSE = "Envelope>";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Только POST (SOAP-вызовы). GET (в т.ч. запрос WSDL) не трогаем.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        // Тело читаем целиком (запрос всегда небольшой). Исходный поток при этом
        // исчерпывается, поэтому дальше по цепочке ВСЕГДА передаём обёртку с кэш-телом —
        // даже когда обрезать нечего.
        byte[] body = request.getInputStream().readAllBytes();
        byte[] trimmed = trimAfterEnvelope(body);
        filterChain.doFilter(new CachedBodyHttpServletRequest(request, trimmed), response);
    }

    /** Обрезает байты после последнего вхождения «Envelope&gt;» (регистр учитывается). */
    static byte[] trimAfterEnvelope(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        int idx = text.lastIndexOf(ENVELOPE_CLOSE);
        if (idx < 0) {
            return body;
        }
        int end = idx + ENVELOPE_CLOSE.length();
        if (end >= text.length()) {
            return body;                                // после конверта уже ничего нет
        }
        return text.substring(0, end).getBytes(StandardCharsets.UTF_8);
    }
}
