package boston.Bus.Map.data;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.HashSet;
import java.util.LinkedList;

import cern.colt.list.IntArrayList;

import com.schneeloch.suffixarray.SuffixArray;

import ags.utils.KdTree.Entry;
import ags.utils.KdTree.WeightedSqrEuclid;
import android.util.Log;
import boston.Bus.Map.R;
import boston.Bus.Map.data.prepopulated.PrepopulatedSuffixArrayRoutes;
import boston.Bus.Map.database.DatabaseHelper;
import boston.Bus.Map.main.UpdateAsyncTask;
import boston.Bus.Map.transit.TransitSource;
import boston.Bus.Map.transit.TransitSystem;
import boston.Bus.Map.ui.ProgressMessage;

public class RoutePool {
	private final DatabaseHelper helper;
	
	/**
	 * A mapping of stop key to route key. Look in sharedStops for the StopLocation
	 */
	private final HashSet<StopLocationGroup> favoriteStops = new HashSet<StopLocationGroup>();

	private final TransitSystem transitSystem;

	private static MyHashMap<StopLocationGroup, StopLocationGroup> stopsByLocation;
	private static MyHashMap<String, RouteConfig> routesByTag;
	private static ArrayList<RouteConfig> routes;

	private static Directions directions;

	private final WeightedSqrEuclid<LocationGroup> kdtree;
	
	private static SuffixArray<StopLocation> stopSuffixArray;
	private static SuffixArray<RouteConfig> routeSuffixArray;
	
	private static MyHashMap<String, StopLocationGroup> stopsByTag;
	
	
	public RoutePool(DatabaseHelper helper, TransitSystem transitSystem) throws IOException {
		this.helper = helper;
		this.transitSystem = transitSystem;

		if (stopsByLocation == null)
		{
			directions = new Directions();

			stopsByLocation = new MyHashMap<StopLocationGroup, StopLocationGroup>();
			routesByTag = new MyHashMap<String, RouteConfig>();
			routes = new ArrayList<RouteConfig>();

			for (TransitSource transitSource : transitSystem.getTransitSources()) {
				for (RouteConfig route : transitSource.makeRoutes(directions)) {
					routesByTag.put(route.getRouteName(), route);
					routes.add(route);
					for (StopLocation stop : route.getStops()) {
						StopLocationGroup locationGroup = stopsByLocation.get(stop);
						if (locationGroup != null) {
							if (locationGroup instanceof MultipleStopLocations) {
								((MultipleStopLocations)locationGroup).addStop(stop);
							}
							else //must be StopLocation
							{
								MultipleStopLocations multipleStopLocations = new MultipleStopLocations((StopLocation)locationGroup, stop);
								stopsByLocation.put(multipleStopLocations, multipleStopLocations);
							}
						}
						else
						{
							stopsByLocation.put(stop, stop);
						}
					}
				}
			}
        }
		
        if (stopsByTag == null) {
        	stopsByTag = new MyHashMap<String, StopLocationGroup>();
        	for (StopLocationGroup stopLocationGroup : stopsByLocation.values()) {
        		if (stopLocationGroup instanceof StopLocation) {
        			stopsByTag.put(stopLocationGroup.getFirstStopTag(), stopLocationGroup);
        		}
        		else
        		{
        			for (StopLocation stop : stopLocationGroup.getStops()) {
        				stopsByTag.put(stop.getStopTag(), stopLocationGroup);
        			}
        		}
        	}
        }
        
        kdtree = new WeightedSqrEuclid<LocationGroup>(2, stopsByLocation.size());
        for (LocationGroup group : stopsByLocation.values()) {
        	kdtree.addPoint(new double[]{group.getLatitudeAsDegrees(), group.getLongitudeAsDegrees()},
        			group);
        }
        
		populateFavorites();
		
		// there could be a conflict if the search happens while this is being created
		// but it's not high priority
		if (routeSuffixArray == null) {
			routeSuffixArray = new SuffixArray<RouteConfig>(true);
			for (RouteConfig routeConfig : routes) {
				routeSuffixArray.add(routeConfig);
			}
			routeSuffixArray.setIndexes(PrepopulatedSuffixArrayRoutes.getRouteIndexes());
		}
		if (stopSuffixArray == null) {
			stopSuffixArray = new SuffixArray<StopLocation>(true);
			for (RouteConfig route : routes) {
				for (StopLocation stop : route.getStops()) {
					stopSuffixArray.add(stop);
				}
			}
			stopSuffixArray.setIndexes(PrepopulatedSuffixArrayRoutes.getStopIndexes());
		}
			
	}
	
	public void saveFavoritesToDatabase()
	{
		helper.saveFavorites(favoriteStops);
	}
	
	
	public RouteConfig get(String routeToUpdate) throws IOException {
		return routesByTag.get(routeToUpdate);
	}

	
	private void populateFavorites() {
		HashSet<String> stopTags = new HashSet<String>();
		helper.populateFavorites(stopTags);
		
		for (StopLocationGroup locationGroup : stopsByLocation.values()) {
			if (locationGroup instanceof MultipleStopLocations) {
				MultipleStopLocations multipleStopLocations = (MultipleStopLocations)locationGroup;
				for (StopLocation stop : multipleStopLocations.getStops()) {
					if (stopTags.contains(stop.getStopTag())) {
						favoriteStops.add(locationGroup);
						break;
					}
				}
			}
			else
			{
				StopLocation stopLocation = (StopLocation)locationGroup;
				if (stopTags.contains(stopLocation.getStopTag())) {
					favoriteStops.add(locationGroup);
				}
			}
		}
	}

	public HashSet<StopLocationGroup> getFavoriteStops() {
		return favoriteStops;
	}

	public boolean isFavorite(LocationGroup locationGroup)
	{
		if (locationGroup instanceof StopLocation) {
			StopLocation stopLocation = (StopLocation)locationGroup;
			return favoriteStops.contains(stopLocation.getStopTag());
		}
		else if (locationGroup instanceof MultipleStopLocations) {
			MultipleStopLocations multipleStopLocations = (MultipleStopLocations)locationGroup;
			for (StopLocation stop : multipleStopLocations.getStops()) {
				if (favoriteStops.contains(stop.getStopTag())) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public int setFavorite(StopLocationGroup locationGroup, boolean isFavorite) {
		LocationGroup group = stopsByLocation.get(locationGroup);

		helper.saveFavorite(group, isFavorite);
		favoriteStops.clear();
		populateFavorites();
		
		return isFavorite ? R.drawable.full_star : R.drawable.empty_star;
	}

	public void clearRecentlyUpdated() {
		for (LocationGroup stop : stopsByLocation.values()) {
			if (stop instanceof StopLocationGroup) {
				((StopLocationGroup)stop).clearRecentlyUpdated();
			}
		}
	}
	
	public ArrayList<LocationGroup> getClosestStops(double centerLatitude,
			double centerLongitude, int maxStops)
	{
		ArrayList<LocationGroup> ret = new ArrayList<LocationGroup>();
		List<Entry<LocationGroup>> list = kdtree.nearestNeighbor(new double[]{centerLatitude, centerLongitude},
				maxStops, false);
		for (Entry<LocationGroup> entry : list) {
			ret.add(entry.value);
		}
		return ret;

	}

	public Directions getDirections() {
		return directions;
	}

	public static SuffixArray<StopLocation> getStopSuffixArray() {
		return stopSuffixArray;
	}

	public static SuffixArray<RouteConfig> getRouteSuffixArray() {
		return routeSuffixArray;
	}

	public static StopLocationGroup getStop(String indexingQuery) {
		return stopsByTag.get(indexingQuery);
	}

}
