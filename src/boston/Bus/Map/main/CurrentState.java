package boston.Bus.Map.main;


import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.MyHashMap;
import boston.Bus.Map.ui.BusOverlay;
import boston.Bus.Map.ui.RouteOverlay;

import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

/**
 * Stores state when MainActivity pauses temporarily
 * @author schneg
 *
 */
public class CurrentState {
	private final long lastUpdateTime;
	private final Locations busLocations;
	private final int updateConstantlyInterval;
	private int selectedRouteIndex;
	private int selectedBusPredictions;
	private final BusOverlay busOverlay;
	private final RouteOverlay routeOverlay;
	private final boolean progressState;
	private final UpdateAsyncTask majorHandler;
	private final boolean locationEnabled;
	
	public CurrentState(Locations busLocations, long lastUpdateTime, int updateConstantlyInterval,
			int selectedRouteIndex, int selectedBusPredictions, BusOverlay busOverlay, RouteOverlay routeOverlay,
			UpdateAsyncTask majorHandler, boolean progressState, boolean locationEnabled) 
	{
		this.busLocations = busLocations;
		this.lastUpdateTime = lastUpdateTime;
		this.updateConstantlyInterval = updateConstantlyInterval;
		this.selectedRouteIndex = selectedRouteIndex;
		this.selectedBusPredictions = selectedBusPredictions;
		this.busOverlay = busOverlay;
		this.routeOverlay = routeOverlay;
		this.progressState = progressState;
		this.majorHandler = majorHandler;
		this.locationEnabled = locationEnabled;
	}

	public long getLastUpdateTime()
	{
		return lastUpdateTime;
	}
	
	public Locations getBusLocations()
	{
		return busLocations;
	}
	
	public void restoreWidgets()
	{
	}

	public boolean getProgressState()
	{
		return progressState;
	}
	
	public int getUpdateConstantlyInterval() {
		return updateConstantlyInterval;
	}

	public BusOverlay getBusOverlay() {
		return busOverlay;
	}

	/**
	 * It's probably unnecessary to clone a new object for this
	 * @param context
	 * @param mapView
	 * @return
	 */
	public BusOverlay cloneBusOverlay(Main context, MapView mapView, MyHashMap<String, String> routeKeysToTitles, float density)
	{
		BusOverlay ret = new BusOverlay(busOverlay, context, mapView, routeKeysToTitles, density);
		
		return ret;
	}

	public int getSelectedRouteIndex()
	{
		return selectedRouteIndex;
	}
	
	public int getSelectedBusPredictions() {
		return selectedBusPredictions;
	}

	public UpdateAsyncTask getMajorHandler() {
		return majorHandler;
	}

	/**
	 * It's probably unnecessary to clone here 
	 * @param projection
	 * @return
	 */
	public RouteOverlay cloneRouteOverlay(Projection projection) {
		RouteOverlay ret = new RouteOverlay(routeOverlay, projection);
		
		return ret;
	}

	public boolean getLocationEnabled() {
		return locationEnabled;
	}
	
}
