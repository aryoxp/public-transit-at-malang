package ap.mobile.malangpublictransport;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ap.mobile.malangpublictransport.base.RouteTransport;
import ap.mobile.malangpublictransport.details.Itinerary;
import ap.mobile.malangpublictransport.details.ItineraryAdapter;
import ap.mobile.malangpublictransport.details.ListLineAdapter;
import ap.mobile.malangpublictransport.ui.CoreProgressDialog;
import ap.mobile.malangpublictransport.ui.UI;
import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
    GoogleMap.OnMapLongClickListener,
    View.OnClickListener,
    View.OnKeyListener,
    Direction.IDirection,
    OnSuccessListener<Location>,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMarkerDragListener {

  private GoogleMap mMap;
  private View selectedRouteCardContainer;
  private ItineraryAdapter itineraryAdapter;
  private RecyclerView rvItinerary;
  private ListLineAdapter listLineAdapter;
  private RecyclerView rvListLine;
  private EditText etDestination;
  private FusedLocationProviderClient mFusedLocationClient;
  private Direction direction;
  private CoreProgressDialog progressDialog;
  private Itinerary itinerary;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    WindowInsetsControllerCompat windowInsetsController =
        ViewCompat.getWindowInsetsController(getWindow().getDecorView());
    if (windowInsetsController == null) {
      return;
    }

    windowInsetsController.setAppearanceLightNavigationBars(false);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

    CDM.cost = Double.parseDouble(
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString("pref_cost", String.valueOf(CDM.cost)));

    setContentView(R.layout.activity_maps);

    Toolbar toolbar = this.findViewById(R.id.app_toolbar);
    this.setSupportActionBar(toolbar);

    this.selectedRouteCardContainer = this.findViewById(R.id.card_container);
    this.selectedRouteCardContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
    this.selectedRouteCardContainer.findViewById(R.id.bt_show_route_options).setOnClickListener(this);

    this.findViewById(R.id.btShowLineList).setOnClickListener(this);
    this.findViewById(R.id.btGo).setOnClickListener(this);

    this.itineraryAdapter = new ItineraryAdapter(new ArrayList<>());
    this.rvItinerary = this.findViewById(R.id.rvItinerary);
    this.rvItinerary.setLayoutManager(new LinearLayoutManager(this));
    this.rvItinerary.setAdapter(this.itineraryAdapter);

    this.listLineAdapter = new ListLineAdapter(this);
    this.rvListLine = this.findViewById(R.id.rvLineList);
    this.rvListLine.setLayoutManager(new LinearLayoutManager(this));
    this.rvListLine.setAdapter(this.listLineAdapter);

    this.etDestination = this.findViewById(R.id.etDestination);
    this.etDestination.setOnKeyListener(this);

    ((SupportMapFragment) Objects.requireNonNull(getSupportFragmentManager()
        .findFragmentById(R.id.map)))
        .getMapAsync(this);

    this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    this.findViewById(R.id.iconGps).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_out));

    // class that handles direction finding functionality
    this.direction = new Direction(this, this);

    // For saving log data
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
      }
    }

  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    this.getMenuInflater().inflate(R.menu.action_menu, menu);
    return true;
  }

  @SuppressLint("NonConstantResourceId")
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      Intent i = new Intent(this, SettingsActivity.class);
      this.startActivity(i);
      return true;
    }
    return true;
  }

  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    this.mMap = googleMap;
    this.listLineAdapter.setGoogleMap(this.mMap);
    this.direction.setGoogleMap(this.mMap);

    try {
      this.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_clean_style));
    } catch (Exception ignored) {}

    this.mMap.setOnMapLongClickListener(this);
    this.mMap.setOnInfoWindowClickListener(this);
    this.mMap.setOnMarkerClickListener(this);
    this.mMap.setOnMarkerDragListener(this);

    // finding user's current location
    if (ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION}, 99);

      return;
    }
    this.moveToCurrentLocation();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 99) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED)
        this.moveToCurrentLocation();
    }
  }

  private void moveToCurrentLocation() {
    if (ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.mFusedLocationClient.getLastLocation().addOnSuccessListener(this);
    }
  }

  @Override
  public void onMapLongClick(@NonNull LatLng latLng) {
    this.direction.getDirections(latLng);
  }

  @Override
  public boolean onMarkerClick(@NonNull Marker marker) {
    return false;
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    // if (this.markerDestination == null) return;
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("pos", this.direction.getSourcePosition().latitude
        + "," + this.direction.getSourcePosition().longitude
        + "," + marker.getPosition().latitude + "," + marker.getPosition().longitude);
    if (clipboard == null || clip == null) return;
    clipboard.setPrimaryClip(clip);
    if (marker.getPosition().latitude == this.direction.getDestinationPosition().latitude &&
      marker.getPosition().longitude == this.direction.getDestinationPosition().longitude)
      this.direction.reloadNetwork();
  }

  @SuppressLint("NotifyDataSetChanged")
  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.bt_show_route_options) {
      this.direction.showRouteOptions();
      return;
    }

    if (view.getId() == R.id.selected_route) {
      if (this.itineraryAdapter.getSteps().size() > 0) this.hideItineraryDetails();
      else {
        this.itineraryAdapter.setSteps(this.itinerary.getSteps());
        this.showItineraryDetails();
      }
      return;
    }

    if(view.getId() == R.id.btShowLineList) {

      this.listLineAdapter.setListLines(this.direction.getListLines());
      this.listLineAdapter.notifyDataSetChanged();

      if (this.rvListLine.getVisibility() == View.GONE) this.rvListLine.setVisibility(View.VISIBLE);
      else this.rvListLine.setVisibility(View.GONE);
      return;
    }

    if(view.getId() == R.id.btGo) {
      String addressString = this.etDestination.getText().toString();
      this.searchLocationAsDestination(addressString);
    }
  }

  @Override
  public void onSuccess(Location location) {
    if (location == null) return;
    Direction.userLocation = new LatLng(location.getLatitude(), location.getLongitude());
    Direction.drawUserLocationMarker(this.mMap);
    this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Direction.userLocation, 15f));
    this.findViewById(R.id.iconGps).clearAnimation();
  }

  @Override
  public void onMarkerDrag(@NonNull Marker marker) {
    marker.setSnippet(marker.getPosition().latitude +", " + marker.getPosition().longitude);
    marker.showInfoWindow();
    if (marker.getTag() == null) return;
    if (marker.getTag().equals("SOURCE"))
      Direction.userLocation = marker.getPosition();
  }

  @Override
  public void onMarkerDragEnd(@NonNull Marker marker) {
    marker.setSnippet(marker.getPosition().latitude +", " + marker.getPosition().longitude);
    marker.showInfoWindow();
    if (marker.getTag() == null) return;
    if (marker.getTag().equals("SOURCE"))
      Direction.userLocation = marker.getPosition();
  }

  @Override
  public void onMarkerDragStart(@NonNull Marker marker) {}

  @Override
  public boolean onKey(View view, int i, KeyEvent keyEvent) {
    if (view.getId() == R.id.etDestination && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
      String addressString = this.etDestination.getText().toString();
      searchLocationAsDestination(addressString);
    }
    return false;
  }

  private void searchLocationAsDestination(String addressString) {
    Geocoder coder = new Geocoder(this.getApplicationContext());
    List<Address> address;
    LatLng pos;
    try {
      // May throw an IOException
      address = coder.getFromLocationName(addressString, 5);
      if (address == null) throw new Exception("Unable to find destination address.");
      Address location = address.get(0);
      pos = new LatLng(location.getLatitude(), location.getLongitude());
      this.onMapLongClick(pos);
    } catch (Exception ex) {
      UI.dialog(this, ex.getMessage()).build().show();
      ex.printStackTrace();
    }
  }

  @Override
  public void onDirectionEvent(Direction.Event e, String message, int progress, int maxProgress) {
    switch (e) {
      case NETWORK_LOADED:
        if (this.mMap != null && this.direction.getDestinationPosition() != null)
          this.onMapLongClick(this.direction.getDestinationPosition());
        break;
      case INITIALIZING:
      case SIMPLIFICATION_BEGIN:
      case SIMPLIFICATION_END:
      case GENERATING_GRAPH:
      case DIJKSTRA_BEGIN:
        if (this.progressDialog == null) {
          this.progressDialog = UI.progress(this, true, 0, message);
          this.progressDialog.show();
        } else this.progressDialog.show(message);
        break;
      case DIJKSTRA_PROGRESS:
        if (this.progressDialog == null) {
          this.progressDialog = UI.progress(this, false, maxProgress, message);
          this.progressDialog.show();
        } else this.progressDialog.setProgress(progress, maxProgress);
        break;
      case DIJKSTRA_END:
        if (this.progressDialog == null) {
          this.progressDialog = UI.progress(this, false, maxProgress, message);
          this.progressDialog.show(message);
        } else this.progressDialog.setProgress(progress, maxProgress);
        this.progressDialog.show(message);
        if (this.direction.getRoutes().size() == 0) {
          this.progressDialog.getDialog().dismiss();
          UI.dialog(this, "Sorry, we are unable to find route to specified destination.").show();
        } else {
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (this.progressDialog.getDialog().isShowing())
              this.progressDialog.getDialog().dismiss();
            this.direction.showRouteOptions();
          }, 1000);
        }
        break;
    }
  }

  @Override
  public void onRouteSelected(RouteTransport route) {
      TextView name = (this.selectedRouteCardContainer.findViewById(R.id.route_name));
      TextView distance = (this.selectedRouteCardContainer.findViewById(R.id.route_distance));
      TextView price = (this.selectedRouteCardContainer.findViewById(R.id.route_price));

      Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
      this.selectedRouteCardContainer.findViewById(R.id.selected_route).setOnClickListener(this);
      this.selectedRouteCardContainer.startAnimation(fadeInAnimation);

      name.setText(route.getNames());
      distance.setText(Helper.humanReadableDistance(route.getDistanceReadable()));
      String priceLabel = "Rp " + String.format(Locale.getDefault(), "%,.0f",
          route.getTotalPrice()).replace(",", ".");
      price.setText(priceLabel);

      this.itinerary = new Itinerary(
        this.direction.getSourcePosition(),
        this.direction.getDestinationPosition(),
        route);

      this.showItineraryDetails();
  }

  @SuppressLint("NotifyDataSetChanged")
  private void showItineraryDetails() {
    final LayoutAnimationController controller =
        AnimationUtils.loadLayoutAnimation(this, R.anim.layout_anim_fall);
    this.itineraryAdapter.setSteps(this.itinerary.getSteps());
    this.rvItinerary.setLayoutAnimation(controller);
    this.itineraryAdapter.notifyDataSetChanged();
    this.rvItinerary.scheduleLayoutAnimation();
    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.rvItinerary.getLayoutParams();
    layoutParams.setMargins(0,0,0,16);
    this.rvItinerary.requestLayout();
  }

  private void hideItineraryDetails() {
    int h = this.rvItinerary.getHeight();
    ValueAnimator slideAnimator = ValueAnimator.ofInt(h, 0).setDuration(200);

    /* We use an update listener which listens to each tick
     * and manually updates the height of the view  */

    slideAnimator.addUpdateListener(anim -> {
      this.rvItinerary.getLayoutParams().height = (int) anim.getAnimatedValue();
      this.rvItinerary.requestLayout();
    });

    /*  We use an animationSet to play the animation  */
    AnimatorSet animationSet = new AnimatorSet();
    animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
    animationSet.play(slideAnimator);

    animationSet.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(@NonNull Animator animator) {}
      @SuppressLint("NotifyDataSetChanged")
      @Override
      public void onAnimationEnd(@NonNull Animator animator) {
        itineraryAdapter.setSteps(new ArrayList<>());
        itineraryAdapter.notifyDataSetChanged();
        rvItinerary.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) rvItinerary.getLayoutParams();
        layoutParams.setMargins(0,0,0,0);
        rvItinerary.requestLayout();
      }
      @Override
      public void onAnimationCancel(@NonNull Animator animator) {}
      @Override
      public void onAnimationRepeat(@NonNull Animator animator) {}
    });
    animationSet.start();
  }

}
