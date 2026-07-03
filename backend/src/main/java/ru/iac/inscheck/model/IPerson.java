package ru.iac.inscheck.model;

import java.time.LocalDate;

/**
 * Запись РС ЕРЗ (таблица IPERSON) — сведения о страховой принадлежности ЗЛ.
 * Используется как ПЗСК* (хронологически ближайшая к периоду поиска запись
 * в комплекте полисов ЗЛ, п.3.5.1.1, п.4.5).
 */
public class IPerson {

    private long id;
    private Long idrow;      // идентификатор строки (связь с Idoc.idrow — документы ЗЛ)
    private Long idmain;
    private Integer smo;
    private Integer vpolis;
    private Integer fpolis;
    private String npolis;
    private String enp;

    private String fam;
    private String im;
    private String ot;
    private Integer w;
    private LocalDate dr;
    private String mr;

    private String ss;

    private LocalDate dvizit;
    private LocalDate dbeg;
    private LocalDate dend;
    private Integer reason;
    private LocalDate ddeath;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getIdrow() { return idrow; }
    public void setIdrow(Long idrow) { this.idrow = idrow; }

    public Long getIdmain() { return idmain; }
    public void setIdmain(Long idmain) { this.idmain = idmain; }

    public Integer getSmo() { return smo; }
    public void setSmo(Integer smo) { this.smo = smo; }

    public Integer getVpolis() { return vpolis; }
    public void setVpolis(Integer vpolis) { this.vpolis = vpolis; }

    public Integer getFpolis() { return fpolis; }
    public void setFpolis(Integer fpolis) { this.fpolis = fpolis; }

    public String getNpolis() { return npolis; }
    public void setNpolis(String npolis) { this.npolis = npolis; }

    public String getEnp() { return enp; }
    public void setEnp(String enp) { this.enp = enp; }

    public String getFam() { return fam; }
    public void setFam(String fam) { this.fam = fam; }

    public String getIm() { return im; }
    public void setIm(String im) { this.im = im; }

    public String getOt() { return ot; }
    public void setOt(String ot) { this.ot = ot; }

    public Integer getW() { return w; }
    public void setW(Integer w) { this.w = w; }

    public LocalDate getDr() { return dr; }
    public void setDr(LocalDate dr) { this.dr = dr; }

    public String getMr() { return mr; }
    public void setMr(String mr) { this.mr = mr; }

    public String getSs() { return ss; }
    public void setSs(String ss) { this.ss = ss; }

    public LocalDate getDvizit() { return dvizit; }
    public void setDvizit(LocalDate dvizit) { this.dvizit = dvizit; }

    public LocalDate getDbeg() { return dbeg; }
    public void setDbeg(LocalDate dbeg) { this.dbeg = dbeg; }

    public LocalDate getDend() { return dend; }
    public void setDend(LocalDate dend) { this.dend = dend; }

    public Integer getReason() { return reason; }
    public void setReason(Integer reason) { this.reason = reason; }

    public LocalDate getDdeath() { return ddeath; }
    public void setDdeath(LocalDate ddeath) { this.ddeath = ddeath; }
}
