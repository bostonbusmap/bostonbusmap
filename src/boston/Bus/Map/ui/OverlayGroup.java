package boston.Bus.Map.ui;

import android.graphics.drawable.Drawable;
import com.schneeloch.bostonbusmap_library.data.RouteTitles;
import boston.Bus.Map.main.Main;

import com.google.android.maps.MapView;
import com.google.common.collect.ImmutableMap;

public class OverlayGroup {
	private LocationOverlay locationOverlay;
	private ImmutableMap<String, RouteOverlay> routeOverlays;
	private BusOverlay busOverlay;
	
	public static final String ROUTE_OVERLAY_KEY = "ROUTE";
	public static final String GET_DIRECTIONS_OVERLAY_KEY = "GET_DIRECTIONS";
	
	public OverlayGroup(BusOverlay busOverlay, LocationOverlay locationOverlay,
			ImmutableMap<String, RouteOverlay> routeOverlays) {
		this.busOverlay = busOverlay;
		this.locationOverlay = locationOverlay;
		ImmutableMap.Builder<String, RouteOverlay> routeOverlayBuilder = ImmutableMap.builder();
		for (String key : routeOverlays.keySet()) {
			RouteOverlay value = routeOverlays.get(key);
			routeOverlayBuilder.put(key, value);
		}
		this.routeOverlays = routeOverlayBuilder.build();
	}
	
	public OverlayGroup(Main main, Drawable busPicture, MapView mapView,
			RouteTitles dropdownRouteKeysToTitles) {
    	busOverlay = new BusOverlay(busPicture, main, mapView, dropdownRouteKeysToTitles);
    	locationOverlay = new LocationOverlay(main, mapView);
    	
    	RouteOverlay routeOverlay = new RouteOverlay(mapView.getProjection());
    	RouteOverlay getDirectionsOverlay = new RouteOverlay(mapView.getProjection());
    	
    	ImmutableMap.Builder<String, RouteOverlay> routeOverlaysBuilder = ImmutableMap.builder();
    	routeOverlaysBuilder.put(ROUTE_OVERLAY_KEY, routeOverlay);
    	routeOverlaysBuilder.put(GET_DIRECTIONS_OVERLAY_KEY, getDirectionsOverlay);
    	this.routeOverlays = routeOverlaysBuilder.build();
	}

	public BusOverlay getBusOverlay() {
		return busOverlay;
	}
	
	public LocationOverlay getMyLocationOverlay() {
		return locationOverlay;
	}
	
	public void nullify() {
		routeOverlays = null;
		busOverlay = null;
		locationOverlay = null;
	}

	public OverlayGroup cloneOverlays(Main context, MapView mapView, 
			RouteTitles dropDownRouteKeysToTitles) {
		
    	final BusOverlay newBusOverlay = new BusOverlay(busOverlay, context, mapView, dropDownRouteKeysToTitles);

    	ImmutableMap.Builder<String, RouteOverlay> newRouteOverlaysBuilder = ImmutableMap.builder();
    	for (String key : routeOverlays.keySet()) {
    		RouteOverlay oldRouteOverlay = routeOverlays.get(key);
    		RouteOverlay newRouteOverlay = new RouteOverlay(oldRouteOverlay, mapView.getProjection());
    		newRouteOverlaysBuilder.put(key, newRouteOverlay);
    	}
    	LocationOverlay newLocationOverlay = new LocationOverlay(context, mapView);
    	
    	return new OverlayGroup(newBusOverlay, newLocationOverlay, newRouteOverlaysBuilder.build());
	}

	public void refreshMapView(MapView mapView) {
    	mapView.getOverlays().clear();
    	for (RouteOverlay routeOverlay : routeOverlays.values()) {
    		mapView.getOverlays().add(routeOverlay);
    	}
    	mapView.getOverlays().add(locationOverlay);
    	mapView.getOverlays().add(busOverlay);
		
	}

	public RouteOverlay getRouteOverlay() {
		return routeOverlays.get(ROUTE_OVERLAY_KEY);
	}
	
	public RouteOverlay getDirectionsOverlay() {
		return routeOverlays.get(GET_DIRECTIONS_OVERLAY_KEY);
	}
}
