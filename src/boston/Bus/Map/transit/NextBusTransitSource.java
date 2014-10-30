package boston.Bus.Map.transit;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.IAlerts;
import boston.Bus.Map.data.IntersectionLocation;
import boston.Bus.Map.data.Location;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.RouteTitles;
import boston.Bus.Map.data.Selection;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.SubwayStopLocation;
import boston.Bus.Map.data.TransitDrawables;
import boston.Bus.Map.data.TransitSourceTitles;
import boston.Bus.Map.data.VehicleLocations;
import boston.Bus.Map.database.Schema;
import boston.Bus.Map.main.Main;
import boston.Bus.Map.main.UpdateAsyncTask;
import boston.Bus.Map.parser.BusPredictionsFeedParser;
import boston.Bus.Map.parser.VehicleLocationsFeedParser;
import boston.Bus.Map.ui.ProgressMessage;
import boston.Bus.Map.util.DownloadHelper;
import boston.Bus.Map.util.LogUtil;
import boston.Bus.Map.util.SearchHelper;
import boston.Bus.Map.util.StreamCounter;

/**
 * A transit source which accesses a NextBus webservice. Override for a specific agency
 * @author schneg
 *
 */
public abstract class NextBusTransitSource implements TransitSource
{
	private final TransitSystem transitSystem;
	
	private static final String prefix = "webservices";
	/**
	 * The XML feed URL
	 */
	private final String mbtaLocationsDataUrlOneRoute;
	private final String mbtaLocationsDataUrlAllRoutes;
	private final String mbtaRouteConfigDataUrl;
	private final String mbtaRouteConfigDataUrlAllRoutes;
	private final String mbtaPredictionsDataUrl;

	private final TransitDrawables drawables;

	private final TransitSourceTitles routeTitles;
	private final RouteTitles allRouteTitles;

	public NextBusTransitSource(TransitSystem transitSystem, 
			TransitDrawables drawables, String agency, TransitSourceTitles routeTitles,
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
		this.allRouteTitles = allRouteTitles;
	}


	@Override
	public void refreshData(RouteConfig routeConfig, Selection selection, int maxStops,
			double centerLatitude, double centerLongitude, VehicleLocations busMapping, 
			RoutePool routePool, Directions directions, Locations locationsObj)
	throws IOException, ParserConfigurationException, SAXException {
		//read data from the URL
		DownloadHelper downloadHelper;
		Selection.Mode mode = selection.getMode();
		if (mode == Selection.Mode.BUS_PREDICTIONS_ONE ||
			mode == Selection.Mode.BUS_PREDICTIONS_STAR ||
			mode == Selection.Mode.BUS_PREDICTIONS_ALL)
		{

			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false, selection);

			//ok, do predictions now
			ImmutableSet<String> routes;
			if (mode == Selection.Mode.BUS_PREDICTIONS_ONE) {
				routes = ImmutableSet.of(routeConfig.getRouteName());
			}
			else
			{
				routes = ImmutableSet.of();
			}
			String url = getPredictionsUrl(locations, maxStops, routes);

			if (url == null)
			{
				return;
			}

			downloadHelper = new DownloadHelper(url);
		}
		else if (mode == Selection.Mode.VEHICLE_LOCATIONS_ONE)
		{
			final String urlString = getVehicleLocationsUrl(locationsObj.getLastUpdateTime(), routeConfig.getRouteName());
			downloadHelper = new DownloadHelper(urlString);
		}
		else
		{
			final String urlString = getVehicleLocationsUrl(locationsObj.getLastUpdateTime(), null);
			downloadHelper = new DownloadHelper(urlString);
		}

		downloadHelper.connect();

		InputStream data = downloadHelper.getResponseData();

		if (mode == Selection.Mode.BUS_PREDICTIONS_ONE ||
				mode == Selection.Mode.BUS_PREDICTIONS_ALL ||
				mode == Selection.Mode.BUS_PREDICTIONS_STAR)
		{
			//bus prediction

			BusPredictionsFeedParser parser = new BusPredictionsFeedParser(routePool, directions);

			parser.runParse(data);
		}
		else 
		{
			//vehicle locations
			//VehicleLocationsFeedParser parser = new VehicleLocationsFeedParser(stream);

			//lastUpdateTime = parser.getLastUpdateTime();

			VehicleLocationsFeedParser parser = new VehicleLocationsFeedParser(drawables, directions, transitSystem.getRouteKeysToTitles());
			parser.runParse(data);

			//get the time that this information is valid until
			locationsObj.setLastUpdateTime(parser.getLastUpdateTime());

			synchronized (busMapping)
			{
				parser.fillMapping(busMapping);

				//delete old buses
				List<VehicleLocations.Key> busesToBeDeleted = Lists.newArrayList();
				for (VehicleLocations.Key id : busMapping.keySet())
				{
					BusLocation busLocation = busMapping.get(id);
					if (busLocation.getLastUpdateInMillis() + 180000 < System.currentTimeMillis())
					{
						//put this old dog to sleep
						busesToBeDeleted.add(id);
					}
				}

				for (VehicleLocations.Key id : busesToBeDeleted)
				{
					busMapping.remove(id);
				}
			}
		}
	}

	protected String getPredictionsUrl(List<Location> locations, int maxStops, Collection<String> routes)
	{
		StringBuilder urlString = new StringBuilder(mbtaPredictionsDataUrl);

		for (Location location : locations)
		{
			if (location instanceof StopLocation)
			{
				StopLocation stopLocation = (StopLocation)location;
				if (stopLocation.getTransitSourceType() == Schema.Routes.enumagencyidBus) {
					if (routes.isEmpty() == false) {
						for (String route : routes) {
							if (stopLocation.hasRoute(route)) {
								urlString.append("&stops=").append(route).append("%7C");
								urlString.append("%7C").append(stopLocation.getStopTag());
							}
						}
					} else {
						for (String stopRoute : stopLocation.getRoutes()) {
							urlString.append("&stops=").append(stopRoute).append("%7C");
							urlString.append("%7C").append(stopLocation.getStopTag());

						}
					}
				}
			}
		}

		//TODO: hard limit this to 150 requests

		return urlString.toString();
	}

	protected String getVehicleLocationsUrl(long time, String route)
	{
		if (route != null)
		{
			return mbtaLocationsDataUrlOneRoute + time + "&r=" + route;
		}
		else
		{
			return mbtaLocationsDataUrlAllRoutes + time;
		}
	}

	protected String getRouteConfigUrl(String route)
	{
		if (route == null)
		{
			return mbtaRouteConfigDataUrlAllRoutes;
		}
		else
		{
			return mbtaRouteConfigDataUrl + route;
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
	public TransitDrawables getDrawables() {
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
