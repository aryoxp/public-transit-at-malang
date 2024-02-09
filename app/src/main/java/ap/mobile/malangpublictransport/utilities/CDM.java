package ap.mobile.malangpublictransport.utilities;

import android.content.Context;
import android.preference.PreferenceManager;

import ap.mobile.malangpublictransport.dijkstra.DijkstraTransport;

public class CDM {

  public static double cost = 4000D;

  public static String getApiBaseUrl(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString("basepath", "http://175.45.187.243/routing");
  }

  public static double getStandardCost() {
    return CDM.cost;
  }

  public static double oneDegreeInMeter() {
    return 111319.9;
  }

  public static int getWalkingDistance(Context context) {
    String cost = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_walkingDistance",
        String.valueOf(CDM.getDefaultWalkingDistance()));
    return Integer.parseInt(cost);
  }

  public static int getDefaultWalkingDistance() {
    return 600;
  } // in meters

  public static boolean useSimplification(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_simplify", false);
  }

  public static int getDefaultSimplificationDistance() {
    return 50;
  }

  public static int getSimplificationDistance(Context context) {
    String epsilon = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_simplify_distance",
        String.valueOf(CDM.getDefaultSimplificationDistance()));
    return Integer.parseInt(epsilon);
  }

  public static DijkstraTransport.Priority getPriority(Context context) {
    DijkstraTransport.Priority priority = PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean("pref_priority", true) ? DijkstraTransport.Priority.COST : DijkstraTransport.Priority.DISTANCE;
    return priority;
  }

}
