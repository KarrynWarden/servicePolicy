package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Корневой элемент SOAP-запроса &lt;GetInsPrkState&gt;, оборачивающий &lt;query&gt;.
 * Соответствует обёртке метода в старом asmx-сервисе.
 */
@XmlRootElement(name = "GetInsPrkState")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetInsPrkState", propOrder = {"query"})
public class GetInsPrkStateRequest {

    @XmlElement(required = true)
    private Query query;

    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }
}
