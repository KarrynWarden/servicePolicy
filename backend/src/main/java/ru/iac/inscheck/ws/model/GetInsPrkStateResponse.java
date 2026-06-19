package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Корневой элемент SOAP-ответа &lt;GetInsPrkStateResponse&gt;, оборачивающий &lt;answer&gt;.
 * Повторяет структуру ответа старого asmx-сервиса (см. пример в постановке, п.3.1.6).
 */
@XmlRootElement(name = "GetInsPrkStateResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetInsPrkStateResponse", propOrder = {"answer"})
public class GetInsPrkStateResponse {

    @XmlElement(required = true)
    private Answer answer;

    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }
}
