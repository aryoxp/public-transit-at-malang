package ap.mobile.malangpublictransport.base;

public class Point {

    protected int idLine;
    protected String id;
    protected int sequence;
    protected boolean stop;
    protected String idInterchange;

    public int getIdLine() { return this.idLine; }
    public int getSequence() { return this.sequence; }
    public Boolean isStop() { return this.stop; }
    public String getIdInterchange() { return this.idInterchange; }

}
