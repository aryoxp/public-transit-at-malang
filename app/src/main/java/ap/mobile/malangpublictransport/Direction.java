package ap.mobile.malangpublictransport;

import android.content.Context;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import ap.mobile.malangpublictransport.details.ListLine;
import ap.mobile.malangpublictransport.dijkstra.DijkstraTask;
import ap.mobile.malangpublictransport.dijkstra.DijkstraTransport;
import ap.mobile.malangpublictransport.douglaspeucker.DouglasPeucker;
import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;
import ap.mobile.malangpublictransport.utilities.MapUtilities;
import ap.mobile.malangpublictransport.utilities.Service;

public class Direction implements Service.IServiceInterface,
    GraphTask.IGraphTask, DijkstraTask.DijkstraTaskListener,
    RouteAdapter.RouteAdapterItemClickListener {

  public static LatLng userLocation, destination;
  private static Marker markerSource, markerDestination;
  private final Context context;
  private final IDirection listener;
  private GoogleMap map;
  private List<Line> lines;
  private List<Interchange> interchanges;
  private List<RouteTransport> routes;
  private MaterialDialog routeOptionsDialog;
  private RouteAdapter routeAdapter;
  private Set<Polyline> routePolylines;
  private Set<Marker> routeMarkers;

  public Direction(Context context, IDirection listener) {
    this.context = context;
    this.listener = listener;
    Service.getManagedPoints(context, this); // load graph raw data
  }

  public LatLng getSourcePosition() {
    return Direction.userLocation;
  }

  public LatLng getDestinationPosition() {
    return Direction.destination;
  }

  public List<RouteTransport> getRoutes() {
    return this.routes;
  }

  public List<ListLine> getListLines() {
    List<ListLine> listLines = new ArrayList<>();
    for(Line line: this.lines) {
      ListLine l = ListLine.from(line, this.interchanges, false);
      listLines.add(l);
    }
    return listLines;
  }

  public enum Event {
    INITIALIZING,
    NETWORK_LOADED,
    GENERATING_GRAPH,
    SIMPLIFICATION_BEGIN,
    SIMPLIFICATION_END,
    DIJKSTRA_BEGIN,
    DIJKSTRA_PROGRESS,
    DIJKSTRA_END
  }

  public interface  IDirection {
    void onDirectionEvent(Direction.Event event, String message, int progress, int max);

    void onRouteSelected(RouteTransport route);
  }

  public static void drawUserLocationMarker(GoogleMap map) {
    // Direction.userLocation = new LatLng(-7.9526921, 112.6144586); // UB
    markerSource = MapUtilities.drawMarker(map,
        Direction.userLocation,
        BitmapDescriptorFactory.HUE_RED,
        "Origin",
        String.format("%s, %s", userLocation.latitude, userLocation.longitude));
    markerSource.setTag("SOURCE");
  }

  public void setGoogleMap(GoogleMap mMap) {
    this.map = mMap;
  }

  @Override
  public void onPointsObtained(ArrayList<Line> lines, ArrayList<Interchange> interchanges) {
    this.lines = lines;
    this.interchanges = interchanges;
    if( this.listener != null )
      this.listener.onDirectionEvent(Event.NETWORK_LOADED, "Network Loaded", 0, 0);
  }

  @Override
  public void onPointsRequestError(String error) {}

  public void reloadNetwork() {
    Service.getManagedPoints(this.context, this); // load graph raw data
  }

  public void getDirections(LatLng source, LatLng destination) {
    if (this.lines == null) {
      Toast.makeText(this.context, "Line data is not ready.", Toast.LENGTH_SHORT).show();
      return;
    }

    if( this.listener != null )
      this.listener.onDirectionEvent(Event.INITIALIZING, "Initializing...", -1, 0);

    if (markerDestination != null) markerDestination.remove();
    markerDestination = MapUtilities.drawMarker(
        this.map, destination, BitmapDescriptorFactory.HUE_GREEN, "Destination",
        destination.latitude + ", " + destination.longitude + "\nTap to show route");
    markerDestination.setTag("DESTINATION");
    markerSource.setVisible(true);
    markerDestination.setVisible(true);

    int radius = CDM.getWalkingDistance(this.context);
    Direction.destination = destination;
    //      lineId,  pointTransport
    HashMap<Integer, PointTransport> nearbyBoard = PointTransport.getNearestPointTransport(this.lines, source, radius);
    HashMap<Integer, PointTransport> nearbyAlight = PointTransport.getNearestPointTransport(this.lines, destination, radius);

    // Reset board and alight flag from starting and stopping points
    for (Line line : this.lines) { // line.path = line.originalPath;
      for (PointTransport p : line.originalPath.values()) {
        p.setIsBoardOrAlight(false);
      }
    }

    for(Line line : this.lines) line.path = line.originalPath;

    if (CDM.useSimplification(this.context)) {
      // Mark nearby boarding and alighting points as one of segments' limit
      // key is line ID
      for (PointTransport nearbyPoint : nearbyBoard.values())
        nearbyPoint.setIsBoardOrAlight(true);
      for (PointTransport nearbyPoint : nearbyAlight.values())
        nearbyPoint.setIsBoardOrAlight(true);

      this.simplifyLines(this.lines);

    }

    new GraphTask(this.lines, this.interchanges,
        new ArrayList<>(nearbyBoard.values()),
        new ArrayList<>(nearbyAlight.values()), this).execute();

    if( this.listener != null )
      this.listener.onDirectionEvent(Event.GENERATING_GRAPH, "Generating graph...", -1, 0);
  }

  public void getDirections(LatLng destination) {
    this.getDirections(Direction.userLocation, destination);
  }

  private void simplifyLines(List<Line> lines) {
    for (Line line : lines) {
      List<List<PointTransport>> segments = new ArrayList<>();
      List<PointTransport> segment = null;
      for (PointTransport p : line.originalPath.values()) {
        if (segment == null) segment = new ArrayList<>();
        if (p.isStop() || p.isBoardOrAlight()) {
          segments.add(segment);
          segment = new ArrayList<>();
        }
        segment.add(p);
      }
      if (segment != null && segment.size() > 1) segments.add(segment);
      SortedMap<Integer, PointTransport> simplifiedPath = new TreeMap<>();
      for (List<PointTransport> s : segments) {
        List<DouglasPeucker.LatLng> ldp = new ArrayList<>();
        Map<String, PointTransport> collection = new HashMap<>();

        // Convert Google Maps LatLng to DP LatLng
        for (PointTransport p : s) {
          collection.put(p.getId(), p);
          ldp.add(new DouglasPeucker.LatLng(p.getId(), p.lat(), p.lng()));
        }

        // do the simplification
        List<DouglasPeucker.LatLng> simSeg = DouglasPeucker.simplify(ldp, CDM.getSimplificationDistance(this.context));

        // Convert it back to list of PointTransport
        for (DouglasPeucker.LatLng p : simSeg) {
          PointTransport pt = collection.get(p.id);
          if (pt != null) simplifiedPath.put(pt.getSequence(), collection.get(p.id));
        }
      }
      line.path = simplifiedPath;
    }
  }

  @Override
  public void onGraphGenerated(Set<PointTransport> points, List<PointTransport> nearbyBoard, List<PointTransport> nearbyAlight) {

    if( this.listener != null )
      this.listener.onDirectionEvent(Event.DIJKSTRA_BEGIN, "Computing directions...", -1, 0);

    GraphTransport graph = new GraphTransport(points);
    DijkstraTransport.Priority priority = CDM.getPriority(this.context);
    DijkstraTask dijkstraTask = new DijkstraTask(graph, nearbyBoard, nearbyAlight, priority, this);
    dijkstraTask.execute();
  }

  @Override
  public void onDijkstraProgress(DijkstraTask.DijkstraReport report) {
    if( this.listener != null )
      this.listener.onDirectionEvent(Event.DIJKSTRA_PROGRESS, "Computing directions...", report.progress, report.total);
  }

  @Override
  public void onDijkstraComplete(ArrayList<RouteTransport> routes) {
    this.routes = routes;
    this.routeAdapter = new RouteAdapter(routes, this);
    if( this.listener != null )
      this.listener.onDirectionEvent(Event.DIJKSTRA_END, "Complete!", routes.size(), routes.size());
  }

  @Override
  public void onDijkstraError(Exception ex) { ex.printStackTrace(); }

  public void showRouteOptions() {
    this.routeOptionsDialog = new MaterialDialog.Builder(this.context)
        .title("Available Routes")
        .adapter(this.routeAdapter, null)
        .positiveText("OK")
        .show();
  }

  @Override
  public void onRouteItemClicked(RouteTransport route) {

    if (this.routeOptionsDialog != null && this.routeOptionsDialog.isShowing())
      this.routeOptionsDialog.dismiss();
    if (this.listener != null)
      this.listener.onRouteSelected(route);

    Marker startMarker = MapUtilities.drawInterchangeMarker(this.map, route.getSource().getLatLng());
    Marker endMarker = MapUtilities.drawInterchangeMarker(this.map, route.getDestination().getLatLng());

    this.drawPath(route.getPath(), startMarker, endMarker);

    LatLngBounds.Builder builder = new LatLngBounds.Builder();
    for (PointTransport point : route.getPath())
      builder.include(point.getLatLng());
    LatLngBounds bounds = builder.build();
    int padding = Helper.toPx(128); // offset from edges of the map in pixels
    this.map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

  }

  private void drawPath(List<PointTransport> path, Marker startMarker, Marker endMarker) {

    if (this.routePolylines != null) for(Polyline p: this.routePolylines) p.remove();
    else this.routePolylines = new HashSet<>();
    if (this.routeMarkers != null) for(Marker m: this.routeMarkers) m.remove();
    else this.routeMarkers = new HashSet<>();

    this.routeMarkers.add(startMarker);
    this.routeMarkers.add(endMarker);

    PolylineOptions startWalkingPolylineOptions = MapUtilities.getWalkingPolylineOptions();
    startWalkingPolylineOptions.add(markerSource.getPosition()).add(startMarker.getPosition());
    this.routePolylines.add(this.map.addPolyline(startWalkingPolylineOptions));

    PolylineOptions polylineOptions = new PolylineOptions().width(10);

    PointTransport prevPoint = null;
    for (PointTransport currentPoint : path) {
      if (prevPoint == null)
        polylineOptions.color(currentPoint.getColor());

      if (prevPoint != null && currentPoint.getIdLine() != prevPoint.getIdLine()) {

        // finish the polyline
        Polyline route = this.map.addPolyline(polylineOptions);
        this.routePolylines.add(route);

        // draw interchange markers
        this.routeMarkers.add(MapUtilities.drawInterchangeMarker(this.map, prevPoint.getLatLng()));
        this.routeMarkers.add(MapUtilities.drawInterchangeMarker(this.map, currentPoint.getLatLng()));

        // draw interchange walking paths
        PolylineOptions transferWalkingPolylineOptions = MapUtilities.getWalkingPolylineOptions();
        transferWalkingPolylineOptions.add(prevPoint.getLatLng()).add(currentPoint.getLatLng());
        this.routePolylines.add(this.map.addPolyline(transferWalkingPolylineOptions));

        // start next line polyline
        polylineOptions = new PolylineOptions().width(10).color(currentPoint.getColor());
      }

      // add current point
      polylineOptions.add(new LatLng(currentPoint.lat(), currentPoint.lng()));
      prevPoint = currentPoint;
    }

    this.routePolylines.add(this.map.addPolyline(polylineOptions));

    assert prevPoint != null;
    this.routeMarkers.add(MapUtilities.drawInterchangeMarker(this.map, prevPoint.getLatLng()));

    PolylineOptions endWalkingPolylineOptions = MapUtilities.getWalkingPolylineOptions();
    endWalkingPolylineOptions.add(markerDestination.getPosition()).add(endMarker.getPosition());
    this.routePolylines.add(this.map.addPolyline(endWalkingPolylineOptions));

  }

}
