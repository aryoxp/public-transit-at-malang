package ap.mobile.malangpublictransport.base;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;

public class GraphTransport {
    private Set<PointTransport> pointTransports;

    public GraphTransport(Set<PointTransport> transportPoints) {
        this.pointTransports = transportPoints;
    }

    public Set<PointTransport> getPointTransports(){
        return this.pointTransports;
    }

    public static Set<PointTransport> build(List<Line> lines, List<Interchange> interchanges) {

        Set<PointTransport> pointSets = new HashSet<>();

        for(Line line: lines) {

            PointTransport prevPoint = null;
            for(PointTransport point : line.path.values()) {
                if(prevPoint == null) {
                    prevPoint = point;
                    pointSets.add(point);
                    continue;
                } else {
                    Double distance = Helper.calculateDistance(prevPoint, point);
                    PointTransport.TransportCost cost = new PointTransport.TransportCost(0D, distance);
                    prevPoint.addDestination(point, cost);
                    point.addSource(prevPoint, cost);
                    prevPoint = point;
                }
                pointSets.add(point);
            }

        }

        // assign points to interchange...
        for(Interchange interchange: interchanges) {
            for (PointTransport point : pointSets) {
                for (String pointId : interchange.pointIds) {
                    if (point.id.equals(pointId))
                        interchange.points.add(point);
                }
            }
        }

        for(Interchange interchange: interchanges) {
            for(PointTransport sPoint : interchange.points) {
                for(PointTransport dPoint: interchange.points) {
                    if(sPoint.id.equals(dPoint.id)) continue;
                    PointTransport.TransportCost cost =
                            new PointTransport.TransportCost(
                                    CDM.getStandardCost(),
                                    0D);
                    sPoint.addDestination(dPoint, cost);
                    dPoint.addSource(sPoint, cost);
                }
            }
        }

        return pointSets;
    }
}
