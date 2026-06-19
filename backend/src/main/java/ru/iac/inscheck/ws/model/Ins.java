package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Сведения о страховой принадлежности (тег &lt;ins&gt;). Состав полей — как в clGIPSV2_Ins. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ins", propOrder = {
        "smo", "vpolis", "fpolis", "npolis", "dvisit", "dbeg", "dend", "reason", "id"
})
public class Ins {

    private String smo;
    private String vpolis;
    private String fpolis;
    private String npolis;
    private String dvisit;
    private String dbeg;
    private String dend;
    private String reason;
    private String id;

    public String getSmo() { return smo; }
    public void setSmo(String smo) { this.smo = smo; }

    public String getVpolis() { return vpolis; }
    public void setVpolis(String vpolis) { this.vpolis = vpolis; }

    public String getFpolis() { return fpolis; }
    public void setFpolis(String fpolis) { this.fpolis = fpolis; }

    public String getNpolis() { return npolis; }
    public void setNpolis(String npolis) { this.npolis = npolis; }

    public String getDvisit() { return dvisit; }
    public void setDvisit(String dvisit) { this.dvisit = dvisit; }

    public String getDbeg() { return dbeg; }
    public void setDbeg(String dbeg) { this.dbeg = dbeg; }

    public String getDend() { return dend; }
    public void setDend(String dend) { this.dend = dend; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
