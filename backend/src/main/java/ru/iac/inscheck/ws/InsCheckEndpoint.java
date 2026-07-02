package ru.iac.inscheck.ws;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;
import ru.iac.inscheck.service.InputValidator;
import ru.iac.inscheck.service.InsCheckService;
import ru.iac.inscheck.service.RequestContext;
import ru.iac.inscheck.ws.model.Answer;
import ru.iac.inscheck.ws.model.Err;
import ru.iac.inscheck.ws.model.GetInsPrkStateRequest;
import ru.iac.inscheck.ws.model.GetInsPrkStateResponse;
import ru.iac.inscheck.ws.model.Query;

/**
 * Слой контроллера (SOAP-эндпоинт). Принимает запрос GetInsPrkState,
 * извлекает сетевой контекст (IP клиента и IP, на который сделан запрос —
 * нужно для поля IPTFOMS в журнале) и делегирует бизнес-логику в сервис.
 *
 * Соответствует методу Service.GetInsPrkState из старого InsCheck.vb.
 */
@Endpoint
public class InsCheckEndpoint {

    private static final String NAMESPACE = "http://tempuri.org/";

    private final InsCheckService service;
    private final InputValidator validator;

    public InsCheckEndpoint(InsCheckService service, InputValidator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "GetInsPrkState")
    @ResponsePayload
    public GetInsPrkStateResponse getInsPrkState(@RequestPayload GetInsPrkStateRequest request,
                                                 MessageContext messageContext) {
        Query q = request.getQuery();

        // Структурная проверка входа (порт GIPSV2_checkInParam) выполняется ДО обращения
        // к БД/транзакции — как в старом сервисе. При ошибке — единственный <err> (код 3/4)
        // без nrec/ack/ins/prk, БД не затрагивается.
        Answer answer;
        InputValidator.StructError se = validator.structuralCheck(q);
        if (se != null) {
            answer = new Answer();
            answer.getErr().add(new Err(String.valueOf(se.code()), se.text()));
        } else {
            answer = service.getInsPrkState(q, buildRequestContext());
        }

        GetInsPrkStateResponse response = new GetInsPrkStateResponse();
        response.setAnswer(answer);
        return response;
    }

    /** Достаёт IP клиента и локальный IP сервера из транспортного контекста HTTP. */
    private RequestContext buildRequestContext() {
        RequestContext ctx = new RequestContext();
        TransportContext transportContext = TransportContextHolder.getTransportContext();
        if (transportContext != null
                && transportContext.getConnection() instanceof HttpServletConnection httpConn) {
            HttpServletRequest http = httpConn.getHttpServletRequest();
            ctx.setRemoteIp(http.getRemoteAddr());
            ctx.setLocalIp(http.getLocalAddr());
            // Логин пользователя ИАС-4 (в старом коде брался из getRealUser / Windows-auth).
            ctx.setLogin(http.getRemoteUser());
        }
        return ctx;
    }
}
