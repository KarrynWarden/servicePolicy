package ru.iac.inscheck.config;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

/**
 * Приводит конверт ответа к формату старого сервиса:
 *  - убирает пустой &lt;SOAP-ENV:Header/&gt; (в оригинале Header отсутствует);
 *  - меняет префикс конверта/Body с SOAP-ENV на soap.
 *
 * На содержимое &lt;answer&gt; (namespace http://tempuri.org/) не влияет.
 * ВНИМАНИЕ: правит SAAJ-дерево напрямую — проверить сырым ответом (curl) после деплоя.
 */
public class SoapEnvelopeInterceptor implements EndpointInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SoapEnvelopeInterceptor.class);
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String TARGET_PREFIX = "soap";

    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) {
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) {
        normalize(messageContext);
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) {
        normalize(messageContext);
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
        // нечего делать
    }

    private void normalize(MessageContext messageContext) {
        if (!(messageContext.getResponse() instanceof SaajSoapMessage saaj)) {
            return;
        }
        try {
            SOAPMessage soap = saaj.getSaajMessage();
            SOAPEnvelope env = soap.getSOAPPart().getEnvelope();

            // 1. Убрать пустой Header — в оригинале его нет.
            SOAPHeader header = env.getHeader();
            if (header != null) {
                header.detachNode();
            }

            // 2. Префикс конверта SOAP-ENV -> soap. Namespace берём У САМОГО конверта,
            //    а не константой: ответ может быть как SOAP 1.1, так и 1.2 (по версии запроса).
            String envNs = env.getNamespaceURI();
            if (envNs == null || envNs.isEmpty()) {
                envNs = SOAP_NS;
            }
            String oldPrefix = env.getPrefix();
            if (!TARGET_PREFIX.equals(oldPrefix)) {
                env.addNamespaceDeclaration(TARGET_PREFIX, envNs);
                env.setPrefix(TARGET_PREFIX);
                if (oldPrefix != null && !oldPrefix.isEmpty()) {
                    env.removeNamespaceDeclaration(oldPrefix);
                }
            }
            // 3. Тот же префикс для Body.
            SOAPBody body = env.getBody();
            if (body != null && !TARGET_PREFIX.equals(body.getPrefix())) {
                body.setPrefix(TARGET_PREFIX);
            }

            soap.saveChanges();
        } catch (Exception e) {
            // Формат конверта — косметика; не роняем ответ из-за неё.
            log.warn("Не удалось нормализовать конверт ответа: {}", e.toString());
        }
    }
}
