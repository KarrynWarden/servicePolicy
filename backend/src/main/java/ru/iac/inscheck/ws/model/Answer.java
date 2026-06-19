package ru.iac.inscheck.ws.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат операции GetInsPrkState (тег &lt;answer&gt;).
 * Состав и порядок полей идентичны старому ответу (clGetInsPrkStateV2_Out):
 * nrec, ack, err[], alg[], ins, prk[], p_disp, p_proph, p_healthc.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "answer", propOrder = {
        "nrec", "ack", "err", "alg", "ins", "prk", "p_disp", "p_proph", "p_healthc"
})
public class Answer {

    private String nrec;
    private String ack;

    @XmlElement(name = "err")
    private List<Err> err = new ArrayList<>();

    @XmlElement(name = "alg")
    private List<String> alg = new ArrayList<>();

    private Ins ins;

    @XmlElement(name = "prk")
    private List<Prk> prk = new ArrayList<>();

    private String p_disp;
    private String p_proph;
    private String p_healthc;

    public String getNrec() { return nrec; }
    public void setNrec(String nrec) { this.nrec = nrec; }

    public String getAck() { return ack; }
    public void setAck(String ack) { this.ack = ack; }

    public List<Err> getErr() { return err; }
    public void setErr(List<Err> err) { this.err = err; }

    public List<String> getAlg() { return alg; }
    public void setAlg(List<String> alg) { this.alg = alg; }

    public Ins getIns() { return ins; }
    public void setIns(Ins ins) { this.ins = ins; }

    public List<Prk> getPrk() { return prk; }
    public void setPrk(List<Prk> prk) { this.prk = prk; }

    public String getP_disp() { return p_disp; }
    public void setP_disp(String p_disp) { this.p_disp = p_disp; }

    public String getP_proph() { return p_proph; }
    public void setP_proph(String p_proph) { this.p_proph = p_proph; }

    public String getP_healthc() { return p_healthc; }
    public void setP_healthc(String p_healthc) { this.p_healthc = p_healthc; }
}
