package ap.mobile.malangpublictransport.base;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;

public class RouteTransport {

  private PointTransport.TransportCost cost;
  private List<PointTransport> path;
  private PointTransport source;
  private PointTransport destination;
  private List<String> lineCodes;
  private List<Integer> colorCodes;

  public RouteTransport(PointTransport source,
                        PointTransport destination,
                        List<PointTransport> path) {
    this.source = source;
    this.destination = destination;
    this.path = path == null ? new ArrayList<>() : path;
    this.cost = destination.getCost();

    this.lineCodes = new ArrayList<>();
    this.colorCodes = new ArrayList<>();

    assert path != null;
    if (path.size() > 0 && !Objects.equals(path.get(0).id, source.id)) path.add(0, source);

    // Log.e("EEE", "RT >> " + this.source.id + ">" + this.source.getLineName() + ":" + this.source.getDirection() + "  /  "
    //     + path.get(0).id + ">" + path.get(0).getLineName() + ":" + path.get(0).getDirection());

    String prevPath = null;
    for (PointTransport p : path) {
      String dir = p.getLineName() + " " + (p.getDirection().charAt(0) == 'O' ? "\u25B6" : "\u25C0");
      if (prevPath == null) {
        prevPath = dir;
        this.lineCodes.add(prevPath);
        this.colorCodes.add(p.getColor());
        continue;
      } else {
        if (prevPath.equals(dir))
          continue;
        prevPath = dir;
        this.lineCodes.add(prevPath);
        this.colorCodes.add(p.getColor());
      }
    }

  }

  public SpannableString getNames() {

    SpannableStringBuilder builder = new SpannableStringBuilder();

    int i = 0;
    for (String line : this.lineCodes) {
      SpannableString lineSpannable = new SpannableString(line);
      lineSpannable.setSpan(new ForegroundColorSpan(this.colorCodes.get(i)),
          0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      lineSpannable.setSpan(new RelativeSizeSpan(0.7f), line.length() - 1, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

      if (i == 0)
        builder.append(lineSpannable);
      else builder.append("  \u203A  ").append(lineSpannable);
      i++;
    }

    return SpannableString.valueOf(builder);

  }

  public int getDistanceReadable() {
    double d = 0;
    PointTransport prevPoint = null;
    for (PointTransport p: this.path) {
      if (prevPoint == null) {
        prevPoint = p;
        continue;
      }
      d += Helper.calculateDistance(p, prevPoint);
      prevPoint = p;
    }
    Log.d("D", this.path.size() + " / " + String.valueOf(d));
    return (int) (d * CDM.oneDegreeInMeter()); // d = this.cost.distance
  }

  public double getTotalPrice() {
    return this.cost.price + CDM.getStandardCost();
  }

  public PointTransport getSource() {
    return source;
  }

  public PointTransport getDestination() {
    return destination;
  }

  public enum ComparatorType {
    PRICE, DISTANCE
  }

  public static Comparator<RouteTransport> getComparator(ComparatorType comparator) {
    if (comparator == ComparatorType.PRICE) {
      return new java.util.Comparator<RouteTransport>() {
        @Override
        public int compare(RouteTransport o1, RouteTransport o2) {
          if (o1.getTotalPrice() < o2.getTotalPrice()) return -1;
          if (o1.getTotalPrice() == o2.getTotalPrice()) {
            return Integer.compare(o1.getDistanceReadable(), o2.getDistanceReadable());
          } else return 1;
        }
      };
    } else {
      return new java.util.Comparator<RouteTransport>() {
        @Override
        public int compare(RouteTransport o1, RouteTransport o2) {
          if (Math.abs(o1.getDistanceReadable() - o2.getDistanceReadable()) <= 100) { // less than 100 meters means equals distance
            return Double.compare(o1.getNumLines(), o2.getNumLines());
          } else return Double.compare(o1.getDistanceReadable(), o2.getDistanceReadable());
        }
      };
    }
  }

  public List<PointTransport> getPath() {
    return this.path;
  }

  public int getNumLines() {
    return this.lineCodes.size();
  }
}
