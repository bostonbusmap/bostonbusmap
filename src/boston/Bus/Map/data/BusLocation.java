package boston.Bus.Map.data;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


import boston.Bus.Map.math.Geometry;
import boston.Bus.Map.transit.TransitSystem;
import boston.Bus.Map.ui.BusDrawable;
import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * This class stores information about the bus. This information is mostly taken
 * from the feed
 */
public class BusLocation implements Location {
	/**
	 * Current latitude of bus, in radians
	 */
	public final float latitude;
	/**
	 * Current longitude of bus, in radians
	 */
	public final float longitude;

	/**
	 * Current latitude of bus, in degrees
	 */
	public final float latitudeAsDegrees;
	/**
	 * Current longitude of bus, in degrees
	 */
	public final float longitudeAsDegrees;

	/**
	 * Unique id for a vehicle. Never null
	 */
	public final String busId;

	private final String routeName;

	/**
	 * Time of last refresh of this bus object
	 */
	public long lastUpdateInMillis;

	/**
	 * When the feed says the information was last updated.
	 */
	public final long lastFeedUpdateInMillis;
	
	/**
	 * Distance in miles of the bus from its previous location, in the x
	 * dimension, squared
	 */
	private float distanceFromLastX;
	/**
	 * Distance in miles of the bus from its previous location, in the y
	 * dimension, squared
	 */
	private float distanceFromLastY;

	/**
	 * What is the heading mentioned for the bus?
	 */
	private String heading;

	/**
	 * Does the bus behave predictably?
	 */
	public final boolean predictable;

	/**
	 * Is the bus inbound, or outbound? This only makes sense if predictable is
	 * true
	 */
	private final String dirTag;

	/**
	 * Inferred bus route
	 */
	private final String inferBusRoute;

	private final Directions directions;

	private final TransitDrawables drawables;
	/**
	 * Distance in pixels between top of bus image and where we want to draw the
	 * arrow
	 */
	private final int arrowTopDiff;

	private final String routeTitle;

	private final boolean disappearAfterRefresh;

	private SimplePredictionView predictionView = SimplePredictionView.empty();
	
	private Alert[] snippetAlerts = new Alert[0];
	
	private static final int LOCATIONTYPE = 1;
	public static final int NO_HEADING = -1;

	public BusLocation(float latitude, float longitude, String id,
			long lastFeedUpdateInMillis, long lastUpdateInMillis, String heading, boolean predictable,
			String dirTag, String inferBusRoute, TransitDrawables drawables,
			String routeName, Directions directions, String routeTitle,
			boolean disappearAfterRefresh, int arrowTopDiff) {
		this.latitude = (float) (latitude * Geometry.degreesToRadians);
		this.longitude = (float) (longitude * Geometry.degreesToRadians);
		this.latitudeAsDegrees = latitude;
		this.longitudeAsDegrees = longitude;
		this.busId = id;
		this.lastUpdateInMillis = lastUpdateInMillis;
		this.lastFeedUpdateInMillis = lastFeedUpdateInMillis;
		this.heading = heading;
		this.predictable = predictable;
		this.dirTag = dirTag;
		this.inferBusRoute = inferBusRoute;
		this.drawables = drawables;
		this.routeName = routeName;
		this.directions = directions;
		this.routeTitle = routeTitle;
		this.disappearAfterRefresh = disappearAfterRefresh;
		this.arrowTopDiff = arrowTopDiff;
	}

	public boolean hasHeading() {
		if (predictable && heading != null) {
			return (getHeading() >= 0);
		} else {
			if (distanceFromLastY == 0 && distanceFromLastX == 0) {
				return false;
			} else {
				return true;
			}
		}
	}

	public int getHeading() {
		if (predictable && heading != null) {
			return Integer.parseInt(heading);
		} else {
			// TODO: this repeats code from getDirection(), make a method to
			// reuse code
			int degrees = Geometry.getDegreesFromSlope(distanceFromLastY,
					distanceFromLastX);
			return degrees;
		}
	}

	public String getDirTag() {
		return dirTag;
	}
	
	/**
	 * 
	 * @return a String describing the direction of the bus, or "" if it can't
	 *         be calculated. For example: E (90 deg)
	 */
	public String getDirection() {
		if (distanceFromLastY == 0 && distanceFromLastX == 0) {
			return "";
		} else {
			int degrees = Geometry.getDegreesFromSlope(distanceFromLastY,
					distanceFromLastX);

			return degrees + " deg (" + convertHeadingToCardinal(degrees) + ")";
		}

	}

	@Override
	public float distanceFrom(double lat2, double lon2) {
		return Geometry.computeCompareDistance(latitude, longitude, lat2, lon2);
	}

	public void movedFrom(float oldLatitude, float oldLongitude) {
		if (oldLatitude == latitude && oldLongitude == longitude) {
			// ignore
			return;
		}
		distanceFromLastX = distanceFrom(latitude, oldLongitude);
		distanceFromLastY = distanceFrom(oldLatitude, longitude);

		if (oldLatitude > latitude) {
			distanceFromLastY *= -1;
		}
		if (oldLongitude > longitude) {
			distanceFromLastX *= -1;
		}
	}

	/**
	 * calculate the distance from the old location
	 * 
	 * @param oldBusLocation
	 */
	public void movedFrom(BusLocation oldBusLocation) {
		movedFrom(oldBusLocation.latitude, oldBusLocation.longitude);
	}

	@Override
	public void addToSnippetAndTitle(RouteConfig routeConfig,
			Location location, RouteTitles routeKeysToTitles, Context context) {
		BusLocation busLocation = (BusLocation) location;

		PredictionView oldPredictionView = predictionView;
		String snippet = oldPredictionView.getSnippet() + "<br />" +
				busLocation.makeSnippet(routeConfig);

		String snippetTitle;
		if (busLocation.predictable) {
			snippetTitle = oldPredictionView.getSnippetTitle() + makeDirection(busLocation.dirTag);
		}
		else
		{
			snippetTitle = oldPredictionView.getSnippetTitle();
		}

		// multiple headings, don't show anything to avoid confusion
		distanceFromLastX = 0;
		distanceFromLastY = 0;
		
		//TODO: support alerts on multiple routes at once
		predictionView = new SimplePredictionView(snippet, snippetTitle, snippetAlerts);
	}

	@Override
	public void makeSnippetAndTitle(RouteConfig routeConfig,
			RouteTitles routeKeysToTitles, Context context) {
		String snippet = makeSnippet(routeConfig);
		String snippetTitle = makeTitle();
		if (routeConfig.getRouteName().equals(routeName))
		{
			snippetAlerts = routeConfig.getAlerts().toArray(new Alert[0]);
		}
		else
		{
			snippetAlerts = new Alert[0];
		}
		
		predictionView = new SimplePredictionView(snippet, snippetTitle, snippetAlerts);
	}

	protected String getBetaWarning()
	{
		return "";
	}
	
	private String makeSnippet(RouteConfig routeConfig) {
		String snippet = getBetaWarning();
		snippet += getBusNumberMessage();

		int secondsAgo = (int) (TransitSystem.currentTimeMillis() - lastFeedUpdateInMillis) / 1000; 
		snippet += "Last update: " + secondsAgo	+ " seconds ago";
		String direction = getDirection();
		if (direction.length() != 0 && predictable == false) {
			snippet += "<br />Estimated direction: " + direction;
		}

		if (predictable && heading != null) {
			snippet += "<br />Heading: " + heading + " deg ("
					+ convertHeadingToCardinal(Integer.parseInt(heading)) + ")";
		} else {
			// TODO: how should we say this?
			// title += "\nUnpredictable";

			if (routeName == null && inferBusRoute != null) {
				snippet += "<br />Estimated route number: " + inferBusRoute;
			}
		}

		return snippet;
	}

	protected String getBusNumberMessage()
	{
		return "Bus number: " + busId + "<br />";
	}

	private String makeTitle() {
		String title = "";
		title += "Route ";
		if (routeTitle == null) {
			title += "not mentioned";
		} else {
			title += routeTitle;
		}

		if (predictable) {
			title += makeDirection(dirTag);
		}

		return title;
	}

	private String makeDirection(String dirTag) {
		String ret = "";

		String directionName = directions.getTitleAndName(dirTag);
		if (directionName != null && directionName.length() != 0) {
			ret += "<br />" + directionName;
		}

		return ret;
	}

	/**
	 * Converts a heading to a cardinal direction string
	 * 
	 * @param degree
	 *            heading in degrees, where 0 is north and 90 is east
	 * @return a direction (for example "N" for north)
	 */
	private String convertHeadingToCardinal(double degree) {
		// shift degree so all directions line up nicely
		degree += 360.0 / 16; // 22.5
		if (degree >= 360) {
			degree -= 360;
		}

		// get an index into the directions array
		int index = (int) (degree / (360.0 / 8.0));
		if (index < 0 || index >= 8) {
			return "calculation error";
		}

		String[] directions = new String[] { "N", "NE", "E", "SE", "S", "SW",
				"W", "NW" };
		return directions[index];
	}

	@Override
	public int getId() {
		return (busId.hashCode() & 0xffffff) | LOCATIONTYPE << 24;
	}

	public String getBusNumber()
	{
		return busId;
	}
	
	public Drawable getDrawable(Context context, boolean shadow,
			boolean isSelected) {
		Drawable drawable = drawables.getVehicle();
		if (shadow == false && hasHeading()) {
			// to make life easier we won't draw shadows except for the bus
			// the tooltip has some weird error where the shadow draws a little
			// left and up from where it should draw

			// the constructor should ignore the arrow and tooltip if these
			// arguments are null
			drawable = new BusDrawable(drawables.getVehicle(), getHeading(), drawables.getArrow(), arrowTopDiff);
		}
		return drawable;
	}

	@Override
	public float getLatitudeAsDegrees() {
		// TODO Auto-generated method stub
		return latitudeAsDegrees;
	}

	@Override
	public float getLongitudeAsDegrees() {
		// TODO Auto-generated method stub
		return longitudeAsDegrees;
	}

	@Override
	public boolean isFavorite() {
		return false;
	}

	/**
	 * The route name
	 * 
	 * @return
	 */
	public String getRouteId() {
		return routeName;
	}

	public boolean isDisappearAfterRefresh() {
		return disappearAfterRefresh;
	}

	public void movedTo(float latitudeAsDegrees, float longitudeAsDegrees) {
		movedFrom(((float)(latitudeAsDegrees * Geometry.degreesToRadians)),
				((float)(longitudeAsDegrees * Geometry.degreesToRadians)));

		distanceFromLastX *= -1;
		distanceFromLastY *= -1;
	}

	@Override
	public boolean containsId(int selectedBusId) {
		return selectedBusId == getId();
	}

	public long getLastUpdateInMillis() {
		return lastUpdateInMillis;
	}

	public void setLastUpdateInMillis(long lastUpdateTime) {
		this.lastUpdateInMillis = lastUpdateTime;
	}

	@Override
	public PredictionView getPredictionView() {
		return predictionView;
	}

	@Override
	public boolean hasMoreInfo() {
		return false;
	}

	@Override
	public boolean hasFavorite() {
		return false;
	}

	@Override
	public boolean hasReportProblem() {
		return true;
	}
}