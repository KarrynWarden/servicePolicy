package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Ошибка обработки (тег &lt;err&gt;): код + текст из Таблицы 4. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "err", propOrder = {"errcode", "errtext"})
public class Err {

    private String errcode;
    private String errtext;

    public Err() {
    }

    public Err(String errcode, String errtext) {
        this.errcode = errcode;
        this.errtext = errtext;
    }

    public String getErrcode() { return errcode; }
    public void setErrcode(String errcode) { this.errcode = errcode; }

    public String getErrtext() { return errtext; }
    public void setErrtext(String errtext) { this.errtext = errtext; }
}
