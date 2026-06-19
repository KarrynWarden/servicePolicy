package ru.iac.inscheck.model;

import java.time.LocalDate;

/**
 * Запись о прикреплении ЗЛ (таблица IPRKDEPT), п.5 постановки.
 * podr формируется как Otdel || lpad(dept,2,'0') || lpad(subdept,2,'0') (п.6.1).
 */
public class IPrkDept {

    private long id;
    private LocalDate dbeg;
    private LocalDate dend;
    private Integer typeprk;
    private Integer mo;
    private String otdel;
    private Integer dept;
    private Integer subdept;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public LocalDate getDbeg() { return dbeg; }
    public void setDbeg(LocalDate dbeg) { this.dbeg = dbeg; }

    public LocalDate getDend() { return dend; }
    public void setDend(LocalDate dend) { this.dend = dend; }

    public Integer getTypeprk() { return typeprk; }
    public void setTypeprk(Integer typeprk) { this.typeprk = typeprk; }

    public Integer getMo() { return mo; }
    public void setMo(Integer mo) { this.mo = mo; }

    public String getOtdel() { return otdel; }
    public void setOtdel(String otdel) { this.otdel = otdel; }

    public Integer getDept() { return dept; }
    public void setDept(Integer dept) { this.dept = dept; }

    public Integer getSubdept() { return subdept; }
    public void setSubdept(Integer subdept) { this.subdept = subdept; }

    /** Код структурной единицы ООООУУПП (п.6.1, п.3.4). */
    public String podr() {
        String o = otdel == null ? "" : otdel;
        String d = String.format("%02d", dept == null ? 0 : dept);
        String s = String.format("%02d", subdept == null ? 0 : subdept);
        return o + d + s;
    }
}
