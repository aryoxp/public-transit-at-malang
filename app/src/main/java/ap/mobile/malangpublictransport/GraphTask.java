package ap.mobile.malangpublictransport;

import android.os.AsyncTask;

import java.util.List;
import java.util.Set;

import ap.mobile.malangpublictransport.base.GraphTransport;
import ap.mobile.malangpublictransport.base.Interchange;
import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;

public class GraphTask extends AsyncTask<Object, Object, Set<PointTransport>> {

    private final List<Line> lines;
    private final List<Interchange> interchanges;
    private final IGraphTask listener;
    private final List<PointTransport> nearbyAlight;
    private final List<PointTransport> nearbyBoard;

    public interface IGraphTask {
        void onGraphGenerated(Set<PointTransport> points, List<PointTransport> nearbyBoard, List<PointTransport> nearbyAlight);
    }

    public GraphTask(List<Line> lines, List<Interchange> interchanges, List<PointTransport> nearbyBoard,
                     List<PointTransport> nearbyAlight, IGraphTask listener) {
        this.lines = lines;
        this.interchanges = interchanges;
        this.listener = listener;
        this.nearbyBoard = nearbyBoard;
        this.nearbyAlight = nearbyAlight;
    }

    @Override
    protected Set<PointTransport> doInBackground(Object... voids) {
        return GraphTransport.build(this.lines, this.interchanges);
    }

    @Override
    protected void onPostExecute(Set<PointTransport> points) {
        if(this.listener != null)
            this.listener.onGraphGenerated(points, this.nearbyBoard, this.nearbyAlight);
        super.onPostExecute(points);
    }
}
