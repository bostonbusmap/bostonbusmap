package com.schneeloch.bostonbusmap_library.transit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableSet;

import com.schneeloch.bostonbusmap_library.data.BusLocation;
import com.schneeloch.bostonbusmap_library.data.Directions;
import com.schneeloch.bostonbusmap_library.data.IAlerts;
import com.schneeloch.bostonbusmap_library.data.ITransitDrawables;
import com.schneeloch.bostonbusmap_library.data.Location;
import com.schneeloch.bostonbusmap_library.data.Locations;
import com.schneeloch.bostonbusmap_library.data.RouteConfig;
import com.schneeloch.bostonbusmap_library.data.RoutePool;
import com.schneeloch.bostonbusmap_library.data.RouteTitles;
import com.schneeloch.bostonbusmap_library.data.Selection;
import com.schneeloch.bostonbusmap_library.data.StopLocation;
import com.schneeloch.bostonbusmap_library.data.TransitSourceTitles;
import com.schneeloch.bostonbusmap_library.data.VehicleLocations;
import com.schneeloch.bostonbusmap_library.database.Schema;
import com.schneeloch.bostonbusmap_library.parser.BusPredictionsFeedParser;
import com.schneeloch.bostonbusmap_library.parser.VehicleLocationsFeedParser;
import com.schneeloch.bostonbusmap_library.util.DownloadHelper;
import com.schneeloch.bostonbusmap_library.util.SearchHelper;

/**
 * A transit source which accesses a NextBus webservice. Override for a specific agency
 * @author schneg
 *
 */
public abstract class NextBusTransitSource implements TransitSource
{
	private final ITransitSystem transitSystem;
	
	private static final String prefix = "webservices";
	/**
	 * The XML feed URL
	 */
	private final String mbtaLocationsDataUrlOneRoute;
	private final String mbtaLocationsDataUrlAllRoutes;
	private final String mbtaRouteConfigDataUrl;
	private final String mbtaRouteConfigDataUrlAllRoutes;
	private final String mbtaPredictionsDataUrl;

	private final ITransitDrawables drawables;

	private final TransitSourceTitles routeTitles;

    /**
     * stop id -> time in millis
     */
    private final ConcurrentHashMap<String, Long> predictionsLastUpdates;
    /**
     * route id -> time in millis
     */
    private final ConcurrentHashMap<String, Long> vehiclesLastUpdates;
    /**
     * Time of last transit source-wide update
     */
    private long wholeSourceVehicleLastUpdate;

    private static final long fetchDelay = 15000;

	public NextBusTransitSource(TransitSystem transitSystem, 
			ITransitDrawables drawables, String agency, TransitSourceTitles routeTitles,
			RouteTitles allRouteTitles)
	{
		this.transitSystem = transitSystem;
		this.drawables = drawables;

		mbtaLocationsDataUrlOneRoute = "http://" + prefix + ".nextbus.com/service/publicXMLFeed?command=vehicleLocations&a=" + agency + "&t=";
		mbtaLocationsDataUrlAllRoutes = "http://" + prefix + ".nextbus.com/service/publicXMLFeed?command=vehicleLocations&a=" + agency + "&t=";
		mbtaRouteConfigDataUrl = "http://" + prefix + ".nextbus.com/service/publicXMLFeed?command=routeConfig&a=" + agency + "&r=";
		mbtaRouteConfigDataUrlAllRoutes = "http://" + prefix + ".nextbus.com/service/publicXMLFeed?command=routeConfig&a=" + agency;
		mbtaPredictionsDataUrl = "http://" + prefix + ".nextbus.com/service/publicXMLFeed?command=predictionsForMultiStops&a=" + agency;
		
		this.routeTitles = routeTitles;
        predictionsLastUpdates = new ConcurrentHashMap<>();
        vehiclesLastUpdates = new ConcurrentHashMap<>();
        wholeSourceVehicleLastUpdate = 0;
	}


	@Override
	public void refreshData(RouteConfig routeConfig, Selection selection, int maxStops,
			double centerLatitude, double centerLongitude, VehicleLocations busMapping, 
			RoutePool routePool, Directions directions, Locations locationsObj)
	throws IOException, ParserConfigurationException, SAXException {
        //read data from the URL
        DownloadHelper downloadHelper;
        Selection.Mode mode = selection.getMode();
        long updateTime = System.currentTimeMillis();
        if (mode == Selection.Mode.BUS_PREDICTIONS_ONE ||
                mode == Selection.Mode.BUS_PREDICTIONS_STAR ||
                mode == Selection.Mode.BUS_PREDICTIONS_ALL) {

            List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false, selection);

            //ok, do predictions now
            ImmutableSet<String> routes;
            if (mode == Selection.Mode.BUS_PREDICTIONS_ONE) {
                routes = ImmutableSet.of(routeConfig.getRouteName());
            } else {
                routes = ImmutableSet.of();
            }
            String url = getPredictionsUrl(locations, maxStops, routes, updateTime);

            if (url == null) {
                return;
            }

            downloadHelper = new DownloadHelper(url);
        } else if (mode == Selection.Mode.VEHICLE_LOCATIONS_ONE) {
            final String urlString = getVehicleLocationsUrl(locationsObj.getLastUpdateTime(), routeConfig.getRouteName(), updateTime);
            vehiclesLastUpdates.put(routeConfig.getRouteName(), updateTime);
            if (urlString == null) {
                return;
            }
            downloadHelper = new DownloadHelper(urlString);
        } else {
            final String urlString = getVehicleLocationsUrl(locationsObj.getLastUpdateTime(), null, updateTime);
            vehiclesLastUpdates.clear();
            wholeSourceVehicleLastUpdate = updateTime;
            if (urlString == null) {
                return;
            }
            downloadHelper = new DownloadHelper(urlString);
        }

        downloadHelper.connect();

        InputStream data = downloadHelper.getResponseData();

        if (mode == Selection.Mode.BUS_PREDICTIONS_ONE ||
                mode == Selection.Mode.BUS_PREDICTIONS_ALL ||
                mode == Selection.Mode.BUS_PREDICTIONS_STAR) {
            //bus prediction

            BusPredictionsFeedParser parser = new BusPredictionsFeedParser(routePool, directions);

            parser.runParse(data);
        } else {
            //vehicle locations
            VehicleLocationsFeedParser parser = new VehicleLocationsFeedParser(directions, transitSystem.getRouteKeysToTitles());
            parser.runParse(data);

            //get the time that this information is valid until
            locationsObj.setLastUpdateTime(parser.getLastUpdateTime());

            long lastUpdateTime = parser.getLastUpdateTime();
            Map<VehicleLocations.Key, BusLocation> newBuses = parser.getNewBuses();

            for (BusLocation bus : newBuses.values()) {
                bus.setLastUpdateInMillis(lastUpdateTime);
            }

            busMapping.update(Schema.Routes.enumagencyidBus, routeTitles.routeTags(), true, newBuses);
        }
    }

	protected String getPredictionsUrl(List<Location> locations, int maxStops, Collection<String> routes, long currentUpdateMillis)
	{
		StringBuilder urlString = new StringBuilder(mbtaPredictionsDataUrl);

        int stopCount = 0;
		for (Location location : locations)
		{
			if (location instanceof StopLocation)
			{
				StopLocation stopLocation = (StopLocation)location;
				if (stopLocation.getTransitSourceType() == Schema.Routes.enumagencyidBus) {
                    Long lastUpdate = predictionsLastUpdates.get(stopLocation.getStopTag());

                    if (lastUpdate == null || (lastUpdate + fetchDelay < currentUpdateMillis)) {
                        if (routes.isEmpty() == false) {
                            for (String route : routes) {
                                if (stopLocation.hasRoute(route)) {
                                    urlString.append("&stops=").append(route).append("%7C");
                                    urlString.append("%7C").append(stopLocation.getStopTag());
                                    predictionsLastUpdates.put(stopLocation.getStopTag(), currentUpdateMillis);
                                    stopCount++;
                                }
                            }
                        } else {
                            for (String stopRoute : stopLocation.getRoutes()) {
                                urlString.append("&stops=").append(stopRoute).append("%7C");
                                urlString.append("%7C").append(stopLocation.getStopTag());
                                predictionsLastUpdates.put(stopLocation.getStopTag(), currentUpdateMillis);
                                stopCount++;
                            }
                        }
                    }
				}
			}
		}

		//TODO: hard limit this to 150 requests

        if (stopCount == 0) {
            return null;
        }
		return urlString.toString();
	}

	protected String getVehicleLocationsUrl(long time, String route, long currentUpdateMillis)
	{
        Long lastUpdate;
        if (route != null) {
            lastUpdate = vehiclesLastUpdates.get(route);
        }
        else {
            lastUpdate = wholeSourceVehicleLastUpdate;
        }
        if (lastUpdate == null || (vehiclesLastUpdates.get(route) + fetchDelay < currentUpdateMillis)) {
            if (route != null)
            {
                return mbtaLocationsDataUrlOneRoute + time + "&r=" + route;
            }
            else
            {
                return mbtaLocationsDataUrlAllRoutes + time;
            }
        }
        else {
            return null;
        }
	}


	@Override
	public boolean hasPaths() {
		return true;
	}


	@Override
	public StopLocation createStop(float lat, float lon, String stopTag,
			String title, String route)
	{
		StopLocation stop = new StopLocation.Builder(lat, lon, stopTag, title).build();
		stop.addRoute(route);
		return stop;
	}

	@Override
	public String searchForRoute(String indexingQuery, String lowercaseQuery)
	{
		return SearchHelper.naiveSearch(indexingQuery, lowercaseQuery, transitSystem.getRouteKeysToTitles());
	}
	
	@Override
	public ITransitDrawables getDrawables() {
		return drawables;
	}

	@Override
	public int getLoadOrder() {
		return 1;
	}
	
	@Override
	public TransitSourceTitles getRouteTitles() {
		return routeTitles;
	}
	
	@Override
	public int[] getTransitSourceIds() {
		return new int[] {Schema.Routes.enumagencyidBus};
	}
	
	@Override
	public boolean requiresSubwayTable() {
		return false;
	}
	
	@Override
	public IAlerts getAlerts() {
		return transitSystem.getAlerts();
	}
	
	@Override
	public String getDescription() {
		return "Bus";
	}
}
