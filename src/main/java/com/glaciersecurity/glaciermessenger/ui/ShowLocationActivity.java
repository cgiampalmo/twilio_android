package com.glaciersecurity.glaciermessenger.ui;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.glaciersecurity.glaciermessenger.R;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

public class ShowLocationActivity extends XmppActivity implements OnMapReadyCallback {

	// Variables needed to initialize a map
	private MapboxMap mapboxMap;
	private MapView mapView;
	// Variables needed to handle location permissions
	private PermissionsManager permissionsManager;
	// Variables needed to add the location engine
	private LocationEngine locationEngine;
	private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
	private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
	private Location loc;
	// Variables needed to listen to location updates

	public static final String ACTION_SHOW_LOCATION = "show_location";



	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Mapbox.getInstance(this, getString(R.string.mapbox_token));

		setContentView(R.layout.activity_show_location);



		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		setSupportActionBar(findViewById(R.id.toolbars));
		configureActionBar(getSupportActionBar());


		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);
	}

	@Override
	public void onMapReady(@NonNull final MapboxMap mapboxMap) {
		this.mapboxMap = mapboxMap;

		mapboxMap.setStyle(Style.TRAFFIC_NIGHT,
				new Style.OnStyleLoaded() {
					@Override
					public void onStyleLoaded(@NonNull Style style) {
						enableLocationComponent(style);
						processViewIntent(getIntent());
					}
				});
	}

	protected void processViewIntent(@NonNull Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (action == null) {
				return;
			}
			switch (action) {
				case "com.glaciersecurity.glaciermessenger.location.show":
					if (intent.hasExtra("longitude") && intent.hasExtra("latitude")) {
						final double longitude = intent.getDoubleExtra("longitude", 0);
						final double latitude = intent.getDoubleExtra("latitude", 0);
						this.loc = new Location("");
						loc.setLatitude(latitude);
						loc.setLongitude(longitude);
					}
					break;
				case ACTION_SHOW_LOCATION:
					if (intent.hasExtra("longitude") && intent.hasExtra("latitude")) {
						final double longitude = intent.getDoubleExtra("longitude", 0);
						final double latitude = intent.getDoubleExtra("latitude", 0);
						this.loc = new Location("");
						loc.setLatitude(latitude);
						loc.setLongitude(longitude);
						mapboxMap.getLocationComponent().forceLocationUpdate(loc);

					}
					break;
			}
		}
	}

	/**
	 * Initialize the Maps SDK's LocationComponent
	 */
	@SuppressWarnings( {"MissingPermission"})
	private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request

// Get an instance of the component
			LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Set the LocationComponent activation options
			LocationComponentActivationOptions locationComponentActivationOptions =
					LocationComponentActivationOptions.builder(this, loadedMapStyle)
							.useDefaultLocationEngine(false)
							.build();

// Activate with the LocationComponentActivationOptions object
			locationComponent.activateLocationComponent(locationComponentActivationOptions);

// Enable to make component visible
			locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
			locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
			locationComponent.setRenderMode(RenderMode.COMPASS);

		//	initLocationEngine();

	}

	@Override
	public void onStart() {
		super.onStart();
		mapView.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mapView.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
    protected void onBackendConnected() {

	}

	@Override
	protected void refreshUiReal() {

	}

}
