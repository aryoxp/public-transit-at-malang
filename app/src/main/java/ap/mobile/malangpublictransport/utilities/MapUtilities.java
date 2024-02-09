package ap.mobile.malangpublictransport.utilities;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Arrays;
import java.util.List;

import ap.mobile.malangpublictransport.R;
import ap.mobile.malangpublictransport.base.PointTransport;

public class MapUtilities {

    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final int PATTERN_GAP_LENGTH_PX = 10;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final List<PatternItem> PATTERN_POLYGON_WALKING = Arrays.asList(DASH, GAP);
    private static final int colorWalking = Color.parseColor("#e53935");
    private static final int colorTransfer = Color.parseColor("#ff9800");

    public static PolylineOptions getWalkingPolylineOptions() {
        return new PolylineOptions()
            .color(colorWalking)
            .pattern(PATTERN_POLYGON_WALKING)
            .width(10f);
    }

    public static Polyline drawPolyline(GoogleMap map, PointTransport[] line, int color) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(color)
                .width(7f);
        for(PointTransport point: line) {
            polylineOptions.add(point.getLatLng());
        }
        return map.addPolyline(polylineOptions);
    }

    public static Marker drawInterchangeMarker(GoogleMap map, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_circle))
                .position(position)
                .anchor(0.5f, 0.5f);
        return map.addMarker(markerOptions);
    }

    public static Marker drawMarker(GoogleMap map, LatLng position, float color, String label, String description) {
        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(color))
                .title(label)
                .snippet(description)
                .position(position)
                .draggable(true);
        return map.addMarker(markerOptions);
    }

    /*
    private void drawGraph(Set<PointTransport> graphPoints) {
        for(Polyline polyline: this.pathPolylinesSimplified)
            polyline.remove();
        this.pathPolylinesSimplified.clear();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(PointTransport point : graphPoints) {

            builder.include(point.getLatLng());

            //Log.d("GCS", point.getId() + " degree: " + point.getDegree());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(point.lat(), point.lng()))
                    .title(point.getId() + ":" + point.getLineName() + " d:" + point.getDegree())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            //if(point.getDegree() == 2)
            //if(chainCandidates.contains(point))
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            //.snippet("Lat: " + point.lat + " Lng: " + lng);
            pointsMarkers.add(this.mMap.addMarker(markerOptions));

            Map<PointTransport, PointTransport.TransportCost> nextPoints = point.getAdjacentTransportPoints();

            for(Map.Entry<PointTransport, PointTransport.TransportCost> entry: nextPoints.entrySet()) {

                PointTransport nextPoint = entry.getKey();

                /*
                LinkedHashMap<PointTransport, PointTransport.TransportCost> subChain = point.getSubChain(nextPoint);
                if(subChain != null) {
                    PolylineOptions subChainPolylineOptions = new PolylineOptions()
                            .add(point.getLatLng())
                            .color(Color.RED)
                            .width(9f);
                    for(PointTransport subChainPoint : subChain.keySet()) {
                        subChainPolylineOptions
                                .add(subChainPoint.getLatLng());
                        builder.include(subChainPoint.getLatLng());
                    }
                    Polyline psc = this.mMap.addPolyline(subChainPolylineOptions);
                    this.subChainPolylines.add(psc);
                }
                */
                /*
                //PointTransport.TransportCost nextCost = entry.getValue();
                PolylineOptions polylineOptions = new PolylineOptions()
                        .add(point.getLatLng())
                        .add(nextPoint.getLatLng())
                        .color(Color.BLUE)
                        .width(9f);
                Polyline ps = this.mMap.addPolyline(polylineOptions);
                this.pathPolylinesSimplified.add(ps);
            }
        }
    }
    */

}
