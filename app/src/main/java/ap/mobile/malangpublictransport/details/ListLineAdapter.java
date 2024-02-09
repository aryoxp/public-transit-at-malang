package ap.mobile.malangpublictransport.details;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ap.mobile.malangpublictransport.R;
import ap.mobile.malangpublictransport.base.PointTransport;
import ap.mobile.malangpublictransport.utilities.MapUtilities;

public class ListLineAdapter extends RecyclerView.Adapter<ListLineAdapter.ViewHolder> {

  private final Context context;
  private List<ListLine> listLines;
  private GoogleMap mMap;
  private final Map<Integer, Polyline> polylineMap = new HashMap<>();
  private final Map<Integer, List<Marker>> lineInterchangeMarkers = new HashMap<>();

  public ListLineAdapter(Context context) {
    this.context = context;
    this.listLines = new ArrayList<>();
  }

  public void setListLines(List<ListLine> listLines) {
    this.listLines = listLines;
  }

  public List<ListLine> getLines() {
    return this.listLines;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(this.context).inflate(R.layout.item_line_list, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    ListLine l = this.listLines.get(position);
    holder.bind(l);
  }

  @Override
  public int getItemCount() {
    return this.listLines.size();
  }

  public void setGoogleMap(GoogleMap mMap) {
    this.mMap = mMap;
  }

  @SuppressLint("NotifyDataSetChanged")
  public void clear() {
    for(Polyline p: this.polylineMap.values()) p.setVisible(false);
    for(ListLine l: this.listLines) l.isShown = false;
    for(List<Marker> lm: this.lineInterchangeMarkers.values()) {
      for (Marker m : lm) m.remove();
    }
    this.lineInterchangeMarkers.clear();
    this.notifyDataSetChanged();
  }

  public class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {

    private final CheckBox cbShow;
    private final View color;
    private final TextView tvName;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      this.color = itemView.findViewById(R.id.v_item_line_color);
      this.tvName = itemView.findViewById(R.id.tv_item_line_name);
      this.cbShow = itemView.findViewById(R.id.cb_item_line_show);
    }

    void bind(ListLine l) {
      this.cbShow.setChecked(l.isShown);
      this.tvName.setText(l.name);
      ViewCompat.setBackgroundTintList(this.color,
          ColorStateList.valueOf(l.color));
      this.cbShow.setOnCheckedChangeListener(this);
      this.cbShow.setTag(l);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
      ListLine listLine = (ListLine) compoundButton.getTag();
      listLine.isShown = b;
      Polyline polyline = polylineMap.get(listLine.id);
      if (polyline == null) {
        PointTransport[] points = listLine.originalPath.values().toArray(new PointTransport[0]);
        polyline = MapUtilities.drawPolyline(mMap, points, listLine.color);
        polylineMap.put(listLine.id, polyline);
      }
      polyline.setVisible(b);

      List<Marker> interchangeMarkers = lineInterchangeMarkers.get(listLine.id);
      if (interchangeMarkers == null) {
        interchangeMarkers = new ArrayList<>();
        for(PointTransport p: listLine.interchanges)
          interchangeMarkers.add(MapUtilities.drawInterchangeMarker(mMap, p.getLatLng()));
        lineInterchangeMarkers.put(listLine.id, interchangeMarkers);
      }
      for(Marker m: interchangeMarkers) m.setVisible(b);

    }
  }
}
