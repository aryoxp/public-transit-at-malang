package ap.mobile.malangpublictransport.dijkstra;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ap.mobile.malangpublictransport.base.GraphTransport;
import ap.mobile.malangpublictransport.base.PointTransport;
import ap.mobile.malangpublictransport.base.RouteTransport;

public class DijkstraTask extends AsyncTask<Void, DijkstraTask.DijkstraReport, ArrayList<RouteTransport>> {

  private final DijkstraTransport.Priority priority;
  private final DijkstraTaskListener listener;
  private final GraphTransport graph;

  private final DijkstraReport report;
  private final List<PointTransport> nearbyAlight;
  private final List<PointTransport> nearbyBoard;

  public DijkstraTask(GraphTransport graph, List<PointTransport> nearbyBoard, List<PointTransport> nearbyAlight, DijkstraTransport.Priority priority, DijkstraTaskListener listener) {
    this.graph = graph;
    this.priority = priority;
    this.listener = listener;
    this.nearbyBoard = nearbyBoard;
    this.nearbyAlight = nearbyAlight;
    this.report = new DijkstraReport();
  }


  @Override
  protected ArrayList<RouteTransport> doInBackground(Void... voids) {

    try {

      this.report.total = this.nearbyBoard.size() * this.nearbyAlight.size();
      ArrayList<RouteTransport> routeTransports = new ArrayList<>();

      this.report.progress = 0;
      for (PointTransport source : this.nearbyBoard) {
        for (PointTransport destination : this.nearbyAlight) {

          DijkstraTransport dijkstra = new DijkstraTransport(this.graph.getPointTransports());
          dijkstra.calculateShortestPathFrom(source, this.priority);

          List<PointTransport> path = (this.priority == DijkstraTransport.Priority.COST) ?
              destination.getCheapestPath() : destination.getShortestPath();

          if (path.size() > 1) {
            path.add(destination);
            // if the first node of the path is not the source node
            // then add source to the path
            if (!Objects.equals(source.getId(), path.get(0).getId())) path.add(0, source);
            // if the first two nodes or two last nodes are on different line
            // then skip the line, it should be already exists
            if (path.get(0).getIdLine() != path.get(1).getIdLine()) continue;
            if (path.get(path.size() - 1).getIdLine() != path.get(path.size() - 2).getIdLine())
              continue;

            RouteTransport routeTransport = new RouteTransport(source, destination, path);
            routeTransports.add(routeTransport);
            this.report.routeTransport = routeTransport;
          }

          this.report.progress++;
          publishProgress(this.report);
        }

      }

      // sort the results
      Collections.sort(routeTransports, RouteTransport.getComparator(
          priority == DijkstraTransport.Priority.COST ?
              RouteTransport.ComparatorType.PRICE :
              RouteTransport.ComparatorType.DISTANCE));

      return routeTransports;

    } catch (Exception ex) {
      if (this.listener != null) this.listener.onDijkstraError(ex);
    }

    return null;
  }

  @Override
  protected void onProgressUpdate(DijkstraReport... values) {
    super.onProgressUpdate(values);
    if (this.listener != null) this.listener.onDijkstraProgress(values[0]);
  }

  @Override
  protected void onPostExecute(ArrayList<RouteTransport> routeTransports) {
    super.onPostExecute(routeTransports);
    if (this.listener != null) this.listener.onDijkstraComplete(routeTransports);
  }

  public interface DijkstraTaskListener {
    void onDijkstraProgress(DijkstraReport report);

    void onDijkstraComplete(ArrayList<RouteTransport> routes);

    void onDijkstraError(Exception ex);
  }

  public static class DijkstraReport {
    public int total;
    public int progress;
    public RouteTransport routeTransport;
  }
}
