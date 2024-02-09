package ap.mobile.malangpublictransport.details;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ap.mobile.malangpublictransport.base.Interchange;
import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;

public class ListLine extends Line {
  public boolean isShown = false;
  public Set<PointTransport> interchanges;

  public static ListLine from(Line line, List<Interchange> interchanges, boolean isShown) {
    ListLine l = new ListLine();
    l.id = line.id;
    l.color = line.color;
    l.cost = line.cost;
    l.name = line.name;
    l.direction = line.direction;
    l.isShown = isShown;
    l.originalPath = line.originalPath;
    l.setInterchanges(interchanges);
    return l;
  }

  private void setInterchanges(List<Interchange> interchanges) {
    this.interchanges = new HashSet<>();
    for(Interchange i : interchanges) {
      for(PointTransport p: this.originalPath.values()) {
        if(i.pointIds.contains(p.getId()))
          this.interchanges.add(p);
      }
    }
  }
}
