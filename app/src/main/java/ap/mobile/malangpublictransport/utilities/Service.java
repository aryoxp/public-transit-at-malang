package ap.mobile.malangpublictransport.utilities;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import ap.mobile.malangpublictransport.R;
import ap.mobile.malangpublictransport.base.Interchange;
import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;

/**
 * Created by Aryo on 29/08/2017.
 */

public class Service {

  public interface IServiceInterface {
    void onPointsObtained(ArrayList<Line> lines, ArrayList<Interchange> interchanges);

    void onPointsRequestError(String error);
  }

    /*
    public static void getPoints(Context context, final IServiceInterface callback) {
        InputStream inputStream = context.getResources().openRawResource(R.raw.points);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            int i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        String json = byteArrayOutputStream.toString();

        ArrayList<PointTransport> points = new ArrayList<>();

        // Display the first 500 characters of the response string.
        //mTextView.setText("Response is: "+ response.substring(0,500));
        //Toast.makeText(MapsActivity.this, "Response is: "+ response.substring(0,100), Toast.LENGTH_SHORT).show();
        try {
            JSONArray response = new JSONArray(json);
            //{"id":"637","lat":"-7.9346600068216","lng":"112.65868753195","l":"1","n":"AL","d":"O","s":"0","a":"1526","i":null}
            for(int i=0;i<response.length();i++) {
                JSONObject jsonPoint = response.getJSONObject(i);
                String id = jsonPoint.getString("id");
                String lat = jsonPoint.getString("lat");
                String lng = jsonPoint.getString("lng");
                String stop = jsonPoint.getString("st");
                String idLine = jsonPoint.getString("l");
                String lineName = jsonPoint.getString("n");
                String direction = jsonPoint.getString("d");
                String color = jsonPoint.getString("c");
                String sequence = jsonPoint.getString("s");
                String adjacentPoints = jsonPoint.getString("a");
                String interchanges = jsonPoint.getString("i");

                if(adjacentPoints.equals("null")) adjacentPoints = null;
                if(interchanges.equals("null")) interchanges = null;

                PointTransport point = new PointTransport(id, Double.valueOf(lat), Double.valueOf(lng), Boolean.valueOf(stop),
                        Integer.valueOf(idLine), lineName, direction, color,
                        Integer.valueOf(sequence), adjacentPoints, interchanges);
                points.add(point);
            }
            if(callback!=null)
                callback.onPointsObtained(points);
        } catch (JSONException e) {
            e.printStackTrace();
            if(callback!= null)
                callback.onPointsRequestError(e.getMessage());
        }

    }
    public static void getPoints(Context context, RequestQueue requestQueue, final IServiceInterface callback) {
        String url = CDM.getApiBaseUrl(context) + "/get-points-json.php";

        JsonArrayRequest pointsJsonRequest = new JsonArrayRequest(Request.Method.GET, url, null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {

                    ArrayList<PointTransport> points = new ArrayList<>();

                    // Display the first 500 characters of the response string.
                    //mTextView.setText("Response is: "+ response.substring(0,500));
                    //Toast.makeText(MapsActivity.this, "Response is: "+ response.substring(0,100), Toast.LENGTH_SHORT).show();
                    try {
                        //{"id":"637","lat":"-7.9346600068216","lng":"112.65868753195","l":"1","n":"AL","d":"O","s":"0","a":"1526","i":null}
                        for(int i=0;i<response.length();i++) {
                            JSONObject jsonPoint = response.getJSONObject(i);
                            String id = jsonPoint.getString("id");
                            String lat = jsonPoint.getString("lat");
                            String lng = jsonPoint.getString("lng");
                            String stop = jsonPoint.getString("st");
                            String idLine = jsonPoint.getString("l");
                            String lineName = jsonPoint.getString("n");
                            String direction = jsonPoint.getString("d");
                            String color = jsonPoint.getString("c");
                            String sequence = jsonPoint.getString("s");
                            String adjacentPoints = jsonPoint.getString("a");
                            String interchanges = jsonPoint.getString("i");

                            if(adjacentPoints.equals("null")) adjacentPoints = null;
                            if(interchanges.equals("null")) interchanges = null;

                            PointTransport point = new PointTransport(id, Double.valueOf(lat), Double.valueOf(lng), Boolean.valueOf(stop),
                                    Integer.valueOf(idLine), lineName, direction, color,
                                    Integer.valueOf(sequence), adjacentPoints, interchanges);
                            points.add(point);
                        }
                        if(callback!=null)
                            callback.onPointsObtained(points);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(callback!= null)
                            callback.onPointsRequestError(e.getMessage());
                    }

                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(callback!= null)
                    callback.onPointsRequestError(error.getMessage());
            }
        });
        requestQueue.add(pointsJsonRequest);
    }
    */

  public static void getManagedPoints(Context context, final IServiceInterface callback) {

    boolean useOfflineData = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_offline", true);

    String rawManagedPointsJson = "";
    if (!useOfflineData) {
      RequestQueue requestQueue = Volley.newRequestQueue(context);
      String url = "https://mgm.ub.ac.id/index.php/admin/m/x/mta/dataApi/getAllData";
      StringRequest stringRequest = new StringRequest(url, response -> {
        parseNetworkData(callback, response);
      }, error -> {
        Log.e("VOLLEY", error.toString());
      });
      requestQueue.add(stringRequest);
    } else {
      rawManagedPointsJson = readRawString(context, R.raw.mta_network);
      parseNetworkData(callback, rawManagedPointsJson);
    }


  }

  private static void parseNetworkData(IServiceInterface callback, String rawManagedPointsJson) {
    try {
      JSONObject response = new JSONObject(rawManagedPointsJson);
      JSONArray linesJson = response.getJSONArray("lines");

      ArrayList<Line> lines = new ArrayList<>();

      for (int i = 0; i < linesJson.length(); i++) {
        JSONObject lineJson = linesJson.getJSONObject(i);
        /* "idline": "1", "name": "AL", "direction": "O", "color": "#FF0000", "path": [] */
        Line line = new Line();
        line.id = Integer.parseInt(lineJson.getString("idline"));
        line.name = lineJson.getString("name");
        line.direction = lineJson.getString("direction");
        line.color = Color.parseColor(lineJson.getString("color"));

        JSONArray pathJson = lineJson.getJSONArray("path");

        for (int j = 0; j < pathJson.length(); j++) {
          JSONObject pointJson = pathJson.getJSONObject(j);
                    /*
                        String id, double lat, double lng, boolean stop, int idLine,
                        String lineName, String direction, String color, int sequence,
                        String adjacentPoints, String interchanges
                     */
          int sequence = Integer.parseInt(pointJson.getString("sequence"));
          PointTransport point = new PointTransport(
              pointJson.getString("idpoint"),
              Double.parseDouble(pointJson.getString("lat")),
              Double.parseDouble(pointJson.getString("lng")),
              pointJson.getString("stop").equals("1"),
              line.id,
              line.name,
              line.direction,
              "#" + Integer.toHexString(line.color),
              sequence,
              null,
              null
          );
          line.originalPath.put(sequence, point);
        }
        lines.add(line);

      }

      ArrayList<Interchange> interchanges = new ArrayList<>();

      JSONArray interchangesJson = response.getJSONArray("interchanges");

      for (int i = 0; i < interchangesJson.length(); i++) {

        JSONObject interchangeJson = interchangesJson.getJSONObject(i);

        Interchange interchange = new Interchange();
        interchange.idInterchange = interchangeJson.getString("idinterchange");
        interchange.name = interchangeJson.getString("name");

        Set<String> pointIds = new HashSet<>();

        JSONArray pointsJson = interchangeJson.getJSONArray("points");

        for (int j = 0; j < pointsJson.length(); j++) {
          JSONObject pointJson = pointsJson.getJSONObject(j);
                    /*
                    "idline": "1",
                    "idpoint": "717",
                    "sequence": "281",
                    "stop": "1",
                    "idinterchange": "4"
                     */
          pointIds.add(pointJson.getString("idpoint"));
        }

        interchange.pointIds.addAll(pointIds);
        interchanges.add(interchange);

      }

      if (callback != null)
        callback.onPointsObtained(lines, interchanges);
    } catch (JSONException e) {
      e.printStackTrace();
      if (callback != null)
        callback.onPointsRequestError(e.getMessage());
    }
  }

  private static String readRawString(Context context, int rawResourceId) {
    InputStream inputStream = context.getResources().openRawResource(rawResourceId);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      int i = inputStream.read();
      while (i != -1) {
        byteArrayOutputStream.write(i);
        i = inputStream.read();
      }
      inputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return byteArrayOutputStream.toString();
  }

}
