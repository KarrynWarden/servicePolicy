package ru.iac.inscheck.model;

/**
 * Подготовленные параметры поиска СП: очищенные/нормализованные реквизиты запроса
 * плюс предрасчитанные метафон-коды ФИО и первые буквы имени/отчества (для раздела H).
 *
 * null означает «реквизит отсутствует/некорректен» — по таким реквизитам алгоритм
 * поиска не выполняется (п.4.2.1).
 */
public class SearchParams {

    private String fam;       // ФИО в верхнем регистре, «-» если пусто
    private String im;
    private String ot;
    private Integer w;        // пол (для проверки соответствия 203)
    private String metaFam;   // метафон ФИО (созвучное)
    private String metaIm;
    private String metaOt;
    private String firstIm;   // первая буква имени (раздел H)
    private String firstOt;   // первая буква отчества (раздел H)
    private String dr;        // дата рождения yyyy-MM-dd
    private String mr;        // место рождения
    private Integer vpolis;
    private String npolis;
    private Integer doctype;
    private String docser;
    private String docnum;
    private String snils;
    private double similarity = 0.8; // порог «схожести» (п.4.2.5)

    public String getFam() { return fam; }
    public void setFam(String fam) { this.fam = fam; }

    public String getIm() { return im; }
    public void setIm(String im) { this.im = im; }

    public String getOt() { return ot; }
    public void setOt(String ot) { this.ot = ot; }

    public Integer getW() { return w; }
    public void setW(Integer w) { this.w = w; }

    public String getMetaFam() { return metaFam; }
    public void setMetaFam(String metaFam) { this.metaFam = metaFam; }

    public String getMetaIm() { return metaIm; }
    public void setMetaIm(String metaIm) { this.metaIm = metaIm; }

    public String getMetaOt() { return metaOt; }
    public void setMetaOt(String metaOt) { this.metaOt = metaOt; }

    public String getFirstIm() { return firstIm; }
    public void setFirstIm(String firstIm) { this.firstIm = firstIm; }

    public String getFirstOt() { return firstOt; }
    public void setFirstOt(String firstOt) { this.firstOt = firstOt; }

    public String getDr() { return dr; }
    public void setDr(String dr) { this.dr = dr; }

    public String getMr() { return mr; }
    public void setMr(String mr) { this.mr = mr; }

    public Integer getVpolis() { return vpolis; }
    public void setVpolis(Integer vpolis) { this.vpolis = vpolis; }

    public String getNpolis() { return npolis; }
    public void setNpolis(String npolis) { this.npolis = npolis; }

    public Integer getDoctype() { return doctype; }
    public void setDoctype(Integer doctype) { this.doctype = doctype; }

    public String getDocser() { return docser; }
    public void setDocser(String docser) { this.docser = docser; }

    public String getDocnum() { return docnum; }
    public void setDocnum(String docnum) { this.docnum = docnum; }

    public String getSnils() { return snils; }
    public void setSnils(String snils) { this.snils = snils; }

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }

    // --- признаки наличия реквизитов (для проверки применимости алгоритма) ---

    public boolean hasFio() { return fam != null && im != null && ot != null; }
    public boolean hasDr() { return dr != null; }
    public boolean hasMr() { return mr != null && !mr.isEmpty(); }
    public boolean hasPolis() { return vpolis != null && npolis != null; }
    public boolean hasDoc() { return doctype != null && docser != null && docnum != null; }
    public boolean hasSnils() { return snils != null && !snils.isEmpty(); }
}
