package boston.Bus.Map.data;

import com.google.common.collect.ImmutableCollection;

import boston.Bus.Map.database.Schema;
import android.graphics.drawable.Drawable;

public class CommuterTrainLocation extends BusLocation {

	public CommuterTrainLocation(float latitude, float longitude, String id,
			long lastFeedUpdateInMillis, long lastUpdateInMillis,
			String heading, boolean predictable, String dirTag,
			String routeName, Directions directions, String routeTitle) {
		super(latitude, longitude, id, lastFeedUpdateInMillis, lastUpdateInMillis,
				heading, predictable, dirTag, routeName,
				directions, routeTitle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getBusNumberMessage() {
		return "Train number: " + busId + "<br />\n";
	}
	
	@Override
	public boolean isDisappearAfterRefresh() {
		return true;
	}
	
	@Override
	public int getTransitSourceType() {
		return Schema.Routes.enumagencyidCommuterRail;
	}
	
	@Override
	protected ImmutableCollection<Alert> getAlerts(IAlerts alerts) {
		return alerts.getAlertsByCommuterRailTripId(busId, routeName);
	}	
}
