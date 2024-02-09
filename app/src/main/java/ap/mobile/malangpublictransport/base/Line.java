package ap.mobile.malangpublictransport.base;

import java.util.SortedMap;
import java.util.TreeMap;

public class Line {

    public int id;
    public String name;
    public String direction;
    public Integer color;

    public Double cost;
    public double distance = 0;

    public SortedMap<Integer, PointTransport> path = new TreeMap<>();
    public SortedMap<Integer, PointTransport> originalPath = new TreeMap<>();

    public Line() {}

    public Line(int id, String name, int color, String direction) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.direction = direction;
    }
}
