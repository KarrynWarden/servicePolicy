package ru.iac.inscheck.config;

import jakarta.servlet.http.HttpServletResponse;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Проверка тела запроса на корректность XML и выдача SOAP Fault с описанием
 * и позицией ошибки — как это делает старый asmx-сервис.
 *
 * Раньше на битый XML отдавалась HTML-страница «Критическая ошибка», по которой
 * нельзя понять, что именно не так с запросом (замечание тестировщика).
 */
final class SoapFaultWriter {

    private static final String NS_11 = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String NS_12 = "http://www.w3.org/2003/05/soap-envelope";

    private SoapFaultWriter() {
    }

    /**
     * Разбирает тело как XML. Возвращает null, если XML корректен,
     * иначе — готовый текст для faultstring (описание + строка/позиция).
     */
    static String checkXml(byte[] body) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            // Защита от XXE: внешние сущности/DTD не подгружаем.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new ByteArrayInputStream(body)), new DefaultHandler());
            return null;
        } catch (SAXParseException e) {
            String msg = e.getMessage() == null ? "не удалось разобрать XML" : e.getMessage();
            return "Серверу не удалось обработать запрос. ---> " + msg
                    + " Строка " + e.getLineNumber() + ", позиция " + e.getColumnNumber() + ".";
        } catch (Exception e) {
            return "Серверу не удалось обработать запрос. ---> " + e.getMessage();
        }
    }

    /** Пишет SOAP Fault в ответ. Версия конверта — та же, что у запроса. */
    static void writeFault(HttpServletResponse response, String reason, boolean soap12) throws IOException {
        String xml = soap12 ? fault12(reason) : fault11(reason);
        byte[] out = xml.getBytes(StandardCharsets.UTF_8);
        response.reset();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);   // штатный код для SOAP Fault
        response.setContentType((soap12 ? "application/soap+xml" : "text/xml") + "; charset=utf-8");
        response.setContentLength(out.length);
        response.getOutputStream().write(out);
        response.getOutputStream().flush();
    }

    private static String fault11(String reason) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<soap:Envelope xmlns:soap=\"" + NS_11 + "\">\n"
                + "  <soap:Body>\n"
                + "    <soap:Fault>\n"
                + "      <faultcode>soap:Server</faultcode>\n"
                + "      <faultstring>" + escape(reason) + "</faultstring>\n"
                + "      <detail />\n"
                + "    </soap:Fault>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";
    }

    private static String fault12(String reason) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<soap:Envelope xmlns:soap=\"" + NS_12 + "\">\n"
                + "  <soap:Body>\n"
                + "    <soap:Fault>\n"
                + "      <soap:Code><soap:Value>soap:Receiver</soap:Value></soap:Code>\n"
                + "      <soap:Reason><soap:Text xml:lang=\"ru\">" + escape(reason) + "</soap:Text></soap:Reason>\n"
                + "      <soap:Detail />\n"
                + "    </soap:Fault>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
