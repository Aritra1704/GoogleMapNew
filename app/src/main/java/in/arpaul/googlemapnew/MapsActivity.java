package in.arpaul.googlemapnew;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arpaul.utilitieslib.LogUtils;
import com.arpaul.utilitieslib.PermissionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import in.arpaul.googlemapnew.dataobject.LocCoordDO;

import static in.arpaul.googlemapnew.BuildConfig.DEBUG;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getSimpleName();

    private SupportMapFragment mapFragment;
    private boolean mIsValidGlVersion;
    private EditText edtLocation;
    private TextView tvLocate, tvSearch, tvCopy, tvPaste;
    private final static String STYLE_ID = "";
    private GoogleApiClient mGoogleApiClient;

    private static final float MAPBOX_BITMAP_MIN_ZOOM_LEVEL = 13.0F;
    private final float zoom = 18F;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        intialiseUIControls();

        bindContols();
    }

    void bindContols() {
        if (Build.VERSION.SDK_INT >= 21) {
            if(new PermissionUtils().checkPermission(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}) != PackageManager.PERMISSION_GRANTED){
                new PermissionUtils().verifyPermission(this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else{
                buildGoogleApiClient();
            }
        } else
            buildGoogleApiClient();

        tvLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mWidth= MapsActivity.this.getResources().getDisplayMetrics().widthPixels / 2;
                int mHeight= MapsActivity.this.getResources().getDisplayMetrics().heightPixels / 2;

                Projection projection = mMap.getProjection();

                // Returns the geographic location that corresponds to a screen location
                LatLng latLng = projection.fromScreenLocation(new Point(mWidth, mHeight));

                // Returns a screen location that corresponds to a geographical coordinate
//                Point screenPosition = projection.toScreenLocation(new LatLng(your_latitude, your_longitude));

                LocCoordDO objLocCoordDO = new LocCoordDO();
                objLocCoordDO.lat = latLng.latitude;
                objLocCoordDO.lng = latLng.longitude;
                String coord = "";
                coord = new Gson().toJson(objLocCoordDO).toString();
//                coord = "Lat: " + objLocCoordDO.lat + "\nLong: " + objLocCoordDO.lng;
                edtLocation.setText(coord);
            }
        });

        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LocCoordDO objLocCoordDO = new Gson().fromJson(edtLocation.getText().toString(), new TypeToken<LocCoordDO>(){}.getType());
                Log.d("LocCoordDO", objLocCoordDO.lat + " " + objLocCoordDO.lng);
                LatLng latlng = new LatLng(objLocCoordDO.lat, objLocCoordDO.lng);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
            }
        });

        tvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipBoard();
            }
        });

        tvPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteFromClipBoard();
            }
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        showLocation();
    }

    private void copyToClipBoard() {
        ClipboardManager clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", edtLocation.getText().toString());
        clipMan.setPrimaryClip(clip);
    }

    private void pasteFromClipBoard() {
        ClipboardManager clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData.Item item = clipMan.getPrimaryClip().getItemAt(0);
        edtLocation.setText("");
        edtLocation.setText(item.getText().toString());
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if(isGpsEnabled()) {
        } else {
            Toast.makeText(MapsActivity.this, "Enable location sevices in your Settings", Toast.LENGTH_SHORT).show();
        }

        if(mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Location location = null;

        if (Build.VERSION.SDK_INT >= 21) {
            if (new PermissionUtils().checkPermission(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}) == PackageManager.PERMISSION_GRANTED) {
                location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            } else {
                new PermissionUtils().verifyPermission(this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        } else
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        this.currentLocation = location;

        showLocation();
//        mvMap.post(new Runnable() {
//            @Override
//            public void run() {
//                if (map != null) {
//                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, (float) zoom));
//                }
//            }
//        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtils.infoLog(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            int location = 0;
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                        grantResult == PackageManager.PERMISSION_GRANTED) {
                    location++;
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        grantResult == PackageManager.PERMISSION_GRANTED) {
                    location++;
                }
            }

            if(location == 2) {
                buildGoogleApiClient();
            } else {
                Toast.makeText(MapsActivity.this, "Allow location permission to access your location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();


        if (DEBUG) Log.i(TAG, "onStart: ");
        if (mGoogleApiClient != null && (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected())) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (DEBUG) Log.i(TAG, "onStop: ");

        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (DEBUG) Log.i(TAG, "onDestroy: ");
    }

    private void showLocation() {
        if(mMap != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            final LatLng latlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latlng).title("Your location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        }
    }

    private boolean isGpsEnabled(){
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isGpsProviderEnabled;
    }

    void intialiseUIControls() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        edtLocation     = (EditText) findViewById(R.id.edtLocation);

        tvLocate        = (TextView) findViewById(R.id.tvLocate);
        tvSearch        = (TextView) findViewById(R.id.tvSearch);
        tvCopy          = (TextView) findViewById(R.id.tvCopy);
        tvPaste         = (TextView) findViewById(R.id.tvPaste);

    }
}
