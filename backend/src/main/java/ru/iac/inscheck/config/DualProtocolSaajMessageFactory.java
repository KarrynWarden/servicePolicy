package ru.iac.inscheck.config;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.SoapMessageCreationException;
import org.springframework.ws.transport.TransportInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Фабрика SOAP-сообщений, понимающая ОБЕ версии конверта — 1.1 и 1.2.
 *
 * Штатная SaajSoapMessageFactory жёстко создаётся под одну версию (по умолчанию 1.1),
 * поэтому запрос с конвертом SOAP 1.2 (xmlns http://www.w3.org/2003/05/soap-envelope,
 * как в примере руководства пользователя) не разбирался вообще — клиент получал HTTP 500.
 * Старый asmx-сервис принимает обе версии и отвечает в версии запроса — повторяем это.
 *
 * Версия определяется по namespace конверта в теле запроса, а НЕ по Content-Type:
 * клиенты (в т.ч. SoapUI из отчёта тестировщика) шлют конверт 1.2 с text/xml, и SAAJ
 * на таком сочетании падает. Поэтому Content-Type для SAAJ подставляем сами — по версии.
 *
 * Версия запроса запоминается в ThreadLocal, чтобы ответ строился в той же версии.
 */
public class DualProtocolSaajMessageFactory extends SaajSoapMessageFactory {

    private static final ThreadLocal<Boolean> SOAP_12_REQUEST = new ThreadLocal<>();

    private MessageFactory soap11;
    private MessageFactory soap12;

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        try {
            soap11 = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            soap12 = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        } catch (SOAPException e) {
            throw new SoapMessageCreationException("Не удалось создать SAAJ MessageFactory: " + e.getMessage(), e);
        }
    }

    /** Сбрасывает запомненную версию (вызывается фильтром по завершении запроса). */
    public static void clearRequestVersion() {
        SOAP_12_REQUEST.remove();
    }

    @Override
    public SaajSoapMessage createWebServiceMessage(InputStream inputStream) throws IOException {
        byte[] body = inputStream.readAllBytes();
        boolean is12 = isSoap12(body);
        SOAP_12_REQUEST.set(is12);

        MessageFactory factory = is12 ? soap12 : soap11;
        MimeHeaders headers = buildHeaders(inputStream, is12);
        try {
            SOAPMessage message = factory.createMessage(headers, new ByteArrayInputStream(body));
            return new SaajSoapMessage(message, factory);
        } catch (SOAPException e) {
            throw new SoapMessageCreationException("Не удалось разобрать SOAP-сообщение: " + e.getMessage(), e);
        }
    }

    /** Ответ строится в версии запроса (старый сервис отвечает в версии запроса). */
    @Override
    public SaajSoapMessage createWebServiceMessage() {
        if (!Boolean.TRUE.equals(SOAP_12_REQUEST.get())) {
            return super.createWebServiceMessage();
        }
        try {
            return new SaajSoapMessage(soap12.createMessage(), soap12);
        } catch (SOAPException e) {
            throw new SoapMessageCreationException("Не удалось создать SOAP 1.2 сообщение: " + e.getMessage(), e);
        }
    }

    /** Ищем namespace конверта SOAP 1.2 в начале документа (до корневого тега тела). */
    static boolean isSoap12(byte[] body) {
        int limit = Math.min(body.length, 4096);   // конверт объявляется в первых байтах
        String head = new String(body, 0, limit, StandardCharsets.UTF_8);
        return head.contains(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
    }

    /**
     * Переносит заголовки транспорта, но Content-Type ставит соответствующий версии —
     * иначе SAAJ отвергает конверт 1.2, присланный с text/xml.
     */
    private MimeHeaders buildHeaders(InputStream inputStream, boolean is12) throws IOException {
        MimeHeaders headers = new MimeHeaders();
        if (inputStream instanceof TransportInputStream tis) {
            for (Iterator<String> names = tis.getHeaderNames(); names.hasNext(); ) {
                String name = names.next();
                if ("Content-Type".equalsIgnoreCase(name) || "Content-Length".equalsIgnoreCase(name)) {
                    continue;   // Content-Type задаём сами, Content-Length неактуален после буферизации
                }
                for (Iterator<String> values = tis.getHeaders(name); values.hasNext(); ) {
                    headers.addHeader(name, values.next());
                }
            }
        }
        headers.setHeader("Content-Type", (is12
                ? SOAPConstants.SOAP_1_2_CONTENT_TYPE
                : SOAPConstants.SOAP_1_1_CONTENT_TYPE) + "; charset=utf-8");
        return headers;
    }
}
