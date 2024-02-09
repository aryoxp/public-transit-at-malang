package ap.mobile.malangpublictransport;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ap.mobile.malangpublictransport.base.GraphTransport;
import ap.mobile.malangpublictransport.base.Interchange;
import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;
import ap.mobile.malangpublictransport.base.RouteTransport;
import ap.mobile.malangpublictransport.dijkstra.DijkstraTask;
import ap.mobile.malangpublictransport.dijkstra.DijkstraTransport;
import ap.mobile.malangpublictransport.douglaspeucker.DouglasPeucker;
import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;
import ap.mobile.malangpublictransport.utilities.MapUtilities;
import ap.mobile.malangpublictransport.utilities.Service;

public class Test implements Service.IServiceInterface, GraphTask.IGraphTask, DijkstraTask.DijkstraTaskListener {
  private String testId;
  private LatLng origin, destination;
  private List<Line> lines;
  private boolean simplify;
  private List<Interchange> interchanges;
  private long start, simplifyTime, graphGenerationTime, end;
  private int npoints, nnearsrc, nneardst;
  private final GoogleMap mMap;
  private final Context ctx;
  private static Marker markerDestination, markerOrigin;
  private final Handler handler;
  private final ITestCallback callback;
  private long simmem;
  private long graphmem;
  private long dijmem;


  Test(Context ctx, GoogleMap map, String testId, ITestCallback callback) {
    this.ctx = ctx;
    this.mMap = map;
    this.handler = new Handler(Looper.getMainLooper());
    this.testId = testId;
    this.callback = callback;
  }

  public static class Route {
    private final LatLng destination;
    private final LatLng origin;

    Route(LatLng origin, LatLng destination) {
      this.origin = origin;
      this.destination = destination;
    }

    public static Test.Route r(LatLng origin, LatLng destination) {
      return new Route(origin, destination);
    }

    public LatLng getDestination() { return this.destination; }
    public LatLng getOrigin() { return this.origin; }
  }
  public interface ITestCallback {
    void onTestComplete();
  }


  public void run(LatLng origin, LatLng destination, boolean simplify) {
    this.origin = origin;
    this.destination = destination;
    this.simplify = simplify;
    Service.getManagedPoints(this.ctx, this);
  }

  public static void runTests() {}

  @Override
  public void onPointsObtained(ArrayList<Line> lines, ArrayList<Interchange> interchanges) {
    this.lines = lines;
    this.interchanges = interchanges;

    if (Test.markerDestination != null) Test.markerDestination.remove();
    Test.markerDestination = MapUtilities.drawMarker(
        this.mMap, this.destination,
        BitmapDescriptorFactory.HUE_AZURE,
        "Destination",
        "Tap to show route\nto this location");
    if (Test.markerOrigin != null) Test.markerOrigin.remove();
    Test.markerOrigin = MapUtilities.drawMarker(
        this.mMap, this.origin,
        BitmapDescriptorFactory.HUE_ORANGE,
        "Origin",
        "Tap to show route\nto this location");


    int radius = CDM.getWalkingDistance(this.ctx);
    HashMap<Integer, PointTransport> nearbyBoard = getNearestPointTransport(this.origin, radius);
    HashMap<Integer, PointTransport> nearbyAlight = getNearestPointTransport(this.destination, radius);

    Toast.makeText(this.ctx,
        nearbyBoard.size() + "/" + nearbyAlight.size(),
        Toast.LENGTH_SHORT).show();

    // Reset board and alight flag
    for (Line line : this.lines) {
      line.path = line.originalPath;
      for (PointTransport p : line.originalPath.values()) {
        p.setIsBoardOrAlight(false);
      }
    }

    // Mark nearby boarding and alighting points as one of segments' limit
    for (Map.Entry<Integer, PointTransport> nearbyPoint : nearbyBoard.entrySet()) {
      nearbyPoint.getValue().setIsBoardOrAlight(true);
      Log.d("Board", "Board: " + nearbyPoint.getKey() + ">" + nearbyPoint.getValue().lat() + "," + nearbyPoint.getValue().lng());
    }
    for (PointTransport nearbyPoint : nearbyAlight.values())
      nearbyPoint.setIsBoardOrAlight(true);

    // Snackbar.make(this.outerLayout, nearbyAlight.values().toString(), Snackbar.LENGTH_SHORT).show();

//    Snackbar.make(this.outerLayout, "ME: " + String.valueOf(MapsActivity.USE_SIMPLIFICATION), Snackbar.LENGTH_SHORT).show();
    Runtime rt = Runtime.getRuntime();
    rt.gc();
    this.start = System.currentTimeMillis();
    if (this.simplify) {
      long simmem = rt.totalMemory() - rt.freeMemory();
      this.simplifyLines(this.lines);
      simmem = (rt.totalMemory() - rt.freeMemory() - simmem);
      this.simmem = simmem;
      Toast.makeText(this.ctx, "Mem Sim: " + simmem, Toast.LENGTH_SHORT).show();
    }
//    else {
//      for(Line line : this.lines) {
//        line.path = line.originalPath;
//      }
//    }
    this.simplifyTime = System.currentTimeMillis();
    rt.gc();
    this.graphmem = rt.totalMemory() - rt.freeMemory();
    new GraphTask(this.lines, this.interchanges,
        new ArrayList<>(nearbyBoard.values()),
        new ArrayList<>(nearbyAlight.values()), this).execute();
  }

  @Override
  public void onPointsRequestError(String error) {

  }

  private HashMap<Integer, PointTransport> getNearestPointTransport(LatLng latLng, int walkingDistance) {
    HashMap<Integer, PointTransport> nearbies = new HashMap<>();
    for (Line line : this.lines) {
      double d = Double.MAX_VALUE;
      PointTransport nearestLinePoint = null;
      for (PointTransport p : line.originalPath.values()) {
        double currentDistance = Helper.calculateDistance(p, latLng.latitude, latLng.longitude);
        if (currentDistance < walkingDistance / CDM.oneDegreeInMeter() && currentDistance < d) {
          d = currentDistance;
          nearestLinePoint = p;
        }
      }
      if (d < Double.MAX_VALUE) {
        Log.d("Nearby", line.id + ":" + line.name + ">" + line.direction);
        nearbies.put(line.id, nearestLinePoint);
      }
    }
    return nearbies;
  }

  public void simplifyLines(List<Line> lines) {
//    for (Polyline p : this.linePolylines) p.remove();
    for (Line line : lines) {
      List<List<PointTransport>> segments = new ArrayList<>();
      List<PointTransport> segment = null;
      for (PointTransport p : line.originalPath.values()) {
        if (segment == null) {
          segment = new ArrayList<>();
        }
        if (p.isStop() || p.isBoardOrAlight()) {
          segments.add(segment);
          segment = new ArrayList<>();
        }
        segment.add(p);
      }
      if (segment != null && segment.size() > 1) segments.add(segment);
      Log.d("line", String.valueOf(segments.size()));
      SortedMap<Integer, PointTransport> simplifiedPath = new TreeMap<>();
      for (List<PointTransport> s : segments) {
        List<DouglasPeucker.LatLng> ldp = new ArrayList<>();
        HashMap<String, PointTransport> collection = new HashMap<>();

        // Convert to DP LatLng
        for (PointTransport p : s) {
          collection.put(p.getId(), p);
          ldp.add(new DouglasPeucker.LatLng(p.getId(), p.lat(), p.lng()));
        }
        List<DouglasPeucker.LatLng> simSeg = DouglasPeucker.simplify(ldp, 50);

        // Convert it back to list of PointTransport
        for (DouglasPeucker.LatLng p : simSeg) {
          PointTransport pt = collection.get(p.id);
          if (pt != null)
            simplifiedPath.put(pt.getSequence(), collection.get(p.id));
        }
      }

//      Polyline p = MapUtilities.drawPolyline(this.mMap, line.originalPath.values().toArray(new PointTransport[0]), line.color);
//      this.linePolylines.add(p);

      line.path = simplifiedPath;
      Log.e("OOO", "AAA: " + line.path.values().size() + "/" + line.originalPath.values().size());
    }
  }

  @Override
  public void onGraphGenerated(Set<PointTransport> points, List<PointTransport> nearbyBoard, List<PointTransport> nearbyAlight) {
    this.graphGenerationTime = System.currentTimeMillis();
    Runtime rt = Runtime.getRuntime();
    this.graphmem = rt.totalMemory() - rt.freeMemory() - this.graphmem;
    this.npoints = points.size();
    this.nnearsrc = nearbyBoard.size();
    this.nneardst = nearbyAlight.size();
    GraphTransport graph = new GraphTransport(points);
    // graph.setTransportPoints(points);
    Toast.makeText(this.ctx,
        "Graph generated from " + points.size() + " points, GMem: " + this.graphmem + ", SMem: " + this.simmem,
        Toast.LENGTH_SHORT).show();
    DijkstraTransport.Priority priority = PreferenceManager.getDefaultSharedPreferences(this.ctx)
        .getBoolean("pref_priority", true) ?
        DijkstraTransport.Priority.COST :
        DijkstraTransport.Priority.DISTANCE;

    rt.gc();
    this.dijmem = rt.totalMemory() - rt.freeMemory();
    DijkstraTask dijkstraTask = new DijkstraTask(
        graph,
        nearbyBoard,
        nearbyAlight,
        priority,
        this);
    dijkstraTask.execute();

//    MaterialDialog dijkstraDialog = new MaterialDialog.Builder(this.ctx)
//        .content("Calculating routes...")
//        .progress(false, 0, true)
//        .show();

  }

  @Override
  public void onDijkstraProgress(DijkstraTask.DijkstraReport report) {

  }

  @Override
  public void onDijkstraComplete(ArrayList<RouteTransport> routes) {
    this.end = System.currentTimeMillis();
    double simDuration = this.simplifyTime - this.start;
    double graphGenDuration = this.graphGenerationTime - this.simplifyTime;
    double dijkstraDuration = this.end - this.graphGenerationTime;
    Runtime rt = Runtime.getRuntime();
    this.dijmem = rt.totalMemory() - rt.freeMemory();
    Toast.makeText(this.ctx, graphGenDuration + " ms, sim: " + simDuration + " ms, dij: " + dijkstraDuration,
        Toast.LENGTH_SHORT).show();
//    FileOutputStream fos = new FileOutputStream("data.txt", Context.MODE_APPEND);
    try {
      String data = "TID: " + this.testId + ", srclat: " + this.origin.latitude + ", srclng: " + this.origin.longitude + ", ";
          data += "dstlat: " + this.destination.latitude + ", dstlng: " + this.destination.longitude + ", ";
          data += "npoints: " + npoints + ", nearsrc: " + nnearsrc + ", neardst: " + nneardst + ", ";
          data += "dp: " + String.valueOf(this.simplify) + ", nroutes: " + routes.size() + ", ";
          data += "gen: " + graphGenDuration + " ms, sim: " + simDuration + " ms, dij: " + dijkstraDuration + ", ";
          data += "graphm: " + this.graphmem + ", simm: " + this.simmem + ", dijm: " + this.dijmem;
          data += "\n";
      File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "data.txt");
      FileOutputStream fos = new FileOutputStream(file, true); // save
      fos.write(data.getBytes());
      fos.close();
      if (this.callback != null) callback.onTestComplete();
    } catch (Exception e) {
      Log.e("FOS", e.getMessage());
    }
  }

  @Override
  public void onDijkstraError(Exception ex) {

  }
}
