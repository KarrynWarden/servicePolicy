package ru.iac.inscheck.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Отдаёт HTML-страницу «Критическая ошибка» на запрос, который не удалось обработать
 * (не разобрался XML: битый тег, недопустимый комментарий, мусор после конверта и т.п.).
 *
 * Старый asmx-сервис в таких случаях возвращал именно эту HTML-страницу, а не пустой
 * ответ. Обычные (разобранные) запросы — включая ответы с errcode — проходят как есть:
 * фильтр подменяет тело только когда сервлет вернул ПУСТО или упал с исключением.
 */
public class CriticalErrorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CriticalErrorFilter.class);

    static final String CRITICAL_HTML =
            "<!DOCTYPE html>\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n"
            + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n"
            + "    <title>Критическая ошибка</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "    <h2>Произошла критическая ошибка. Повторите операцию позже.</h2>\n"
            + "</body>\n"
            + "</html>";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        boolean failed = false;
        try {
            filterChain.doFilter(request, wrapper);
        } catch (Exception e) {
            // Разбор запроса упал (например, MessageDispatcherServlet не смог создать SOAP-сообщение).
            log.error("Критическая ошибка при обработке SOAP-запроса: {}", e.toString(), e);
            failed = true;
        } finally {
            // Версия SOAP запоминается на время запроса — снимаем, чтобы она не досталась
            // следующему запросу на этом же потоке пула.
            DualProtocolSaajMessageFactory.clearRequestVersion();
        }

        if (!failed && wrapper.getContentSize() > 0) {
            wrapper.copyBodyToResponse();   // нормальный ответ (SOAP, в т.ч. с errcode) — как есть
            return;
        }

        // Пусто/ошибка — запрос не разобрался. Отдаём HTML, как старый сервис.
        response.reset();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/html; charset=utf-8");
        byte[] body = CRITICAL_HTML.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.getOutputStream().flush();
    }
}
