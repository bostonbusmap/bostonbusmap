package com.schneeloch.bostonbusmap_library.data;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;

import com.schneeloch.bostonbusmap_library.database.Schema;

public class CommuterTrainLocation extends BusLocation {

	public CommuterTrainLocation(float latitude, float longitude, String id,
                                 long lastFeedUpdateInMillis, Optional<Integer> heading,
                                 String routeName, String headsign) {
		super(latitude, longitude, id, lastFeedUpdateInMillis,
				heading, routeName, headsign);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getBusNumberMessage() {
		return "Trip: " + busId + "<br />\n";
	}
	
	@Override
	protected ImmutableCollection<Alert> getAlerts(IAlerts alerts) {
		return alerts.getAlertsByCommuterRailTripId(busId, routeName);
	}

	public Schema.Routes.SourceId getVehicleSourceId() {
		return Schema.Routes.SourceId.CommuterRail;
	}
}
