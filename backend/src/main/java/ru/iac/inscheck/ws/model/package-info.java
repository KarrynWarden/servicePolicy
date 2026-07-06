/**
 * JAXB-модель SOAP-сообщений сервиса InsCheck.
 *
 * Namespace и имена тегов полностью повторяют старый сервис (ASP.NET asmx,
 * namespace http://tempuri.org/), чтобы вход/выход остались идентичными для
 * существующих клиентов. elementFormDefault = QUALIFIED — как в исходном asmx.
 */
@XmlSchema(
        namespace = "http://tempuri.org/",
        elementFormDefault = XmlNsForm.QUALIFIED,
        // Пустой префикс — namespace по умолчанию (как в старом сервисе): теги без ns2.
        xmlns = { @XmlNs(prefix = "", namespaceURI = "http://tempuri.org/") }
)
package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
