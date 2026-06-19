package ru.iac.inscheck.service;

/**
 * Сетевой контекст запроса, нужный для журналирования (раздел 3 постановки):
 * IP клиента (поле IP), локальный IP сервера (поле IPTFOMS) и логин пользователя.
 */
public class RequestContext {

    private String remoteIp;
    private String localIp;
    private String login;

    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String remoteIp) { this.remoteIp = remoteIp; }

    public String getLocalIp() { return localIp; }
    public void setLocalIp(String localIp) { this.localIp = localIp; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
}
