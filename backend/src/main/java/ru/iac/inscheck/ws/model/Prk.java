package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Сведения о прикреплении (тег &lt;prk&gt;). Состав полей — как в clGIPSV2_Prk. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "prk", propOrder = {"mo", "podr", "modt"})
public class Prk {

    private String mo;
    private String modt;
    private String podr;

    public String getMo() { return mo; }
    public void setMo(String mo) { this.mo = mo; }

    public String getModt() { return modt; }
    public void setModt(String modt) { this.modt = modt; }

    public String getPodr() { return podr; }
    public void setPodr(String podr) { this.podr = podr; }
}
