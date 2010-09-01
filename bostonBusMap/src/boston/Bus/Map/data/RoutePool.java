package boston.Bus.Map.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;
import boston.Bus.Map.R;
import boston.Bus.Map.database.DatabaseHelper;

public class RoutePool {
	private final DatabaseHelper helper;
	
	
	private final LinkedList<String> priorities = new LinkedList<String>();
	private final HashMap<String, RouteConfig> pool = new HashMap<String, RouteConfig>();
	private final HashMap<String, StopLocation> sharedStops = new HashMap<String, StopLocation>();
	private final HashMap<String, String> favoriteStops = new HashMap<String, String>();

	private final String[] supportedRoutes;
	
	
	private static final int MAX_ROUTES = 50;
	
	public RoutePool(DatabaseHelper helper, String[] supportedRoutes) {
		this.helper = helper;
		this.supportedRoutes = supportedRoutes;
		
		helper.populateFavorites(favoriteStops);
	}
	
	public void fillInFavoritesRoutes()
	{
		ArrayList<String> stopsToRemove = new ArrayList<String>();
		for (String stop : favoriteStops.keySet()) {
			String route = favoriteStops.get(stop);
			
			try
			{
				if (route == null)
				{
					//le ugh
					for (String supportedRoute : supportedRoutes)
					{
						RouteConfig routeConfig = get(supportedRoute);
						if (routeConfig == null)
						{
							//database hasn't been refreshed yet?
							return;
						}
						StopLocation stopLocation = routeConfig.getStop(stop);
						if (stopLocation != null)
						{
							route = stopLocation.getFirstRoute();
							stopLocation.setFavorite(true);
							break;
						}
					}
				}
				
				if (route != null)
				{
					//Log.v("BostonBusMap", "getting route " + (route == null ? "null" : route) +
					//		" because favorite stop " + stop + " requested it");
					RouteConfig routeConfig = get(route);
					sharedStops.put(stop, routeConfig.getStop(stop));
				}
				else
				{
					//this shouldn't happen. We can't find the route the favorite belongs to. Just remove it
					stopsToRemove.add(stop);
				}
			}
			catch (IOException e)
			{
				Log.e("BostonBusMap", "Error getting route " + route + ": " + e.getMessage());
			}
		}
		
		for (String stop : stopsToRemove)
		{
			favoriteStops.remove(stop);
		}
		
		helper.saveFavorites(favoriteStops);
	}

	
	/**
	 * In the future, this may be necessary to implement. Currently all route data is shipped with the app
	 * 
	 * @param route
	 * @return
	 */
	public boolean isMissingRouteInfo(String route) {
		return false;
	}

	public RouteConfig get(String routeToUpdate) throws IOException {
		debugStateOfPool();
		
		RouteConfig routeConfig = pool.get(routeToUpdate);
		if (routeConfig != null)
		{
			return routeConfig;
		}
		else
		{
			synchronized (helper)
			{
				routeConfig = helper.getRoute(routeToUpdate, sharedStops);
				if (routeConfig == null)
				{
					return null;
				}
				else
				{
					if (priorities.size() >= MAX_ROUTES)
					{
						removeARoute(priorities.get(0));
					}

					addARoute(routeToUpdate, routeConfig);

					return routeConfig;
				}
			}
		}
	}

	private void debugStateOfPool() {
		ArrayList<String> routes = new ArrayList<String>(pool.size());
		routes.addAll(pool.keySet());
		Collections.sort(routes);
		
		StringBuffer joinable = new StringBuffer();
		for (String route : routes)
		{
			joinable.append(route).append(", ");
		}
		
		Log.v("BostonBusMap", "routes currently in pool: " + joinable);
	}

	private void addARoute(String routeToUpdate, RouteConfig routeConfig) {
		priorities.add(routeToUpdate);
		pool.put(routeToUpdate, routeConfig);
	}

	private void removeARoute(String routeToRemove) {
		RouteConfig routeConfig = pool.get(routeToRemove);
		priorities.remove(routeToRemove);
		pool.remove(routeToRemove);
		
		//TODO: can this be done faster?
		if (routeConfig == null)
		{
			return;
		}
		//remove all stops from sharedStops,
		//unless that stop is owned by another route also which is currently in the pool
		for (StopLocation stopLocation : routeConfig.getStops())
		{
			boolean keepStop = false;
			for (String route : stopLocation.getRoutes())
			{
				if (pool.containsKey(route))
				{
					//keep this stop
					keepStop = true;
					break;
				}
			}
			
			if (favoriteStops.containsKey(stopLocation.getStopTag()))
			{
				keepStop = true;
			}
			
			if (keepStop == false)
			{
				sharedStops.remove(stopLocation.getStopTag());
			}
		}
	}

	public void writeToDatabase(HashMap<String, RouteConfig> map, boolean wipe) throws IOException {
		helper.saveMapping(map, wipe, sharedStops);
		for (String route : map.keySet())
		{
			//TODO: we just saved to database and then reloaded from database. This is inefficient
			refreshRoute(route);
		}
	}

	private void refreshRoute(String route) throws IOException {
		removeARoute(route);
		get(route);
	}

	public ArrayList<String> routeInfoNeedsUpdating(String[] supportedRoutes) {
		//TODO: what if another route gets added later, and we want to download it from the server and add it?
		return helper.routeInfoNeedsUpdating(supportedRoutes);
	}

	public StopLocation[] getFavoriteStops() {
		ArrayList<StopLocation> ret = new ArrayList<StopLocation>(favoriteStops.size());
		
		for (String stopTag : favoriteStops.keySet())
		{
			StopLocation stopLocation = sharedStops.get(stopTag);
			if (stopLocation == null)
			{
				fillInFavoritesRoutes();
				stopLocation = sharedStops.get(stopTag);
			}
			
			if (stopLocation != null)
			{
				ret.add(stopLocation);
			}
		}
		
		return ret.toArray(new StopLocation[0]);
	}

	public int toggleFavorite(StopLocation location) {
		String stopTag = location.getStopTag();
		if (favoriteStops.containsKey(stopTag))
		{
			location.setFavorite(false);
			favoriteStops.remove(stopTag);
			helper.saveFavorite(stopTag, location.getFirstRoute(), false);
			return R.drawable.empty_star;
		}
		else
		{
			location.setFavorite(true);
			favoriteStops.put(stopTag, location.getFirstRoute());
			helper.saveFavorite(stopTag, location.getFirstRoute(), true);
			return R.drawable.full_star;
		}

	}

}
