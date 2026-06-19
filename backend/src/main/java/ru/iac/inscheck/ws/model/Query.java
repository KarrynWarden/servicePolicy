package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Входные параметры операции GetInsPrkState (тег &lt;query&gt;).
 * Набор и порядок тегов идентичны старому сервису (clGetInsPrkStateV2_In).
 * Все поля строковые — формат/значность проверяются на уровне сервиса
 * (как и в старом коде, где БД получала Varchar2).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "query", propOrder = {
        "nrec", "date1", "date2", "type_org", "code_org",
        "fam", "im", "ot", "w", "dr", "vpolis", "npolis",
        "doctype", "docser", "docnum", "snils", "mr"
})
public class Query {

    @XmlElement(required = true)
    private String nrec;
    @XmlElement(required = true)
    private String date1;
    @XmlElement(required = true)
    private String date2;
    @XmlElement(required = true)
    private String type_org;
    @XmlElement(required = true)
    private String code_org;
    private String fam;
    private String im;
    private String ot;
    private String w;
    private String dr;
    private String vpolis;
    private String npolis;
    private String doctype;
    private String docser;
    private String docnum;
    private String snils;
    private String mr;

    public String getNrec() { return nrec; }
    public void setNrec(String nrec) { this.nrec = nrec; }

    public String getDate1() { return date1; }
    public void setDate1(String date1) { this.date1 = date1; }

    public String getDate2() { return date2; }
    public void setDate2(String date2) { this.date2 = date2; }

    public String getType_org() { return type_org; }
    public void setType_org(String type_org) { this.type_org = type_org; }

    public String getCode_org() { return code_org; }
    public void setCode_org(String code_org) { this.code_org = code_org; }

    public String getFam() { return fam; }
    public void setFam(String fam) { this.fam = fam; }

    public String getIm() { return im; }
    public void setIm(String im) { this.im = im; }

    public String getOt() { return ot; }
    public void setOt(String ot) { this.ot = ot; }

    public String getW() { return w; }
    public void setW(String w) { this.w = w; }

    public String getDr() { return dr; }
    public void setDr(String dr) { this.dr = dr; }

    public String getVpolis() { return vpolis; }
    public void setVpolis(String vpolis) { this.vpolis = vpolis; }

    public String getNpolis() { return npolis; }
    public void setNpolis(String npolis) { this.npolis = npolis; }

    public String getDoctype() { return doctype; }
    public void setDoctype(String doctype) { this.doctype = doctype; }

    public String getDocser() { return docser; }
    public void setDocser(String docser) { this.docser = docser; }

    public String getDocnum() { return docnum; }
    public void setDocnum(String docnum) { this.docnum = docnum; }

    public String getSnils() { return snils; }
    public void setSnils(String snils) { this.snils = snils; }

    public String getMr() { return mr; }
    public void setMr(String mr) { this.mr = mr; }
}
