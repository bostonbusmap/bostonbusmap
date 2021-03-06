package com.schneeloch.bostonbusmap_library.parser;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.schneeloch.bostonbusmap_library.data.HubwayStopData;
import com.schneeloch.bostonbusmap_library.data.HubwayStopLocation;
import com.schneeloch.bostonbusmap_library.data.Locations;
import com.schneeloch.bostonbusmap_library.data.PredictionStopLocationPair;
import com.schneeloch.bostonbusmap_library.data.RouteConfig;
import com.schneeloch.bostonbusmap_library.data.SimplePrediction;
import com.schneeloch.bostonbusmap_library.data.StopLocation;
import com.schneeloch.bostonbusmap_library.parser.gson.stationInfo.InfoRoot;
import com.schneeloch.bostonbusmap_library.parser.gson.stationInfo.InfoStation;
import com.schneeloch.bostonbusmap_library.parser.gson.stationStatus.StatusRoot;
import com.schneeloch.bostonbusmap_library.parser.gson.stationStatus.StatusStation;
import com.schneeloch.bostonbusmap_library.transit.HubwayTransitSource;

/**
 * Created by schneg on 9/1/13.
 */
public class HubwayParser {
	private final RouteConfig routeConfig;
	private final List<HubwayStopData> hubwayStopData = Lists.newArrayList();

	public HubwayParser(RouteConfig routeConfig) {
		this.routeConfig = routeConfig;
	}

	public void runParse(Reader infoReader, Reader statusReader)  {
		BufferedReader bufferedInfoReader = new BufferedReader(infoReader, 2048);
		BufferedReader bufferedStatusReader = new BufferedReader(statusReader, 2048);

		InfoRoot infoRoot = new Gson().fromJson(bufferedInfoReader, InfoRoot.class);
		StatusRoot statusRoot = new Gson().fromJson(bufferedStatusReader, StatusRoot.class);

		Map<Integer, StatusStation> statusLookup = Maps.newHashMap();
		for (StatusStation station : statusRoot.data.stations) {
			statusLookup.put(station.station_id, station);
		}

		for (InfoStation infoStation : infoRoot.data.stations) {
			StatusStation statusStation = statusLookup.get(infoStation.station_id);
			if (statusStation != null) {
				String tag = HubwayTransitSource.stopTagPrefix + statusStation.station_id;
				boolean installed = statusStation.is_installed == 1;
				boolean locked = !(statusStation.is_renting == 1 && statusStation.is_returning == 1);
				HubwayStopData data = new HubwayStopData(
						tag, String.valueOf(statusStation.station_id), infoStation.lat, infoStation.lon, infoStation.name,
						String.valueOf(statusStation.num_bikes_available), String.valueOf(statusStation.num_docks_available), locked, installed
				);
				hubwayStopData.add(data);
			}
		}
	}

    /**
     * For Hubway we need to update the stop list here, we don't receive it ahead of time.
     */
    public void addMissingStops(Locations locations) throws IOException {
        ImmutableMap.Builder<String, StopLocation> builder = ImmutableMap.builder();

        for (HubwayStopData data : hubwayStopData) {
            HubwayStopLocation.HubwayBuilder hubwayBuilder = new HubwayStopLocation.HubwayBuilder(data.latitude, data.longitude, data.tag, data.name, Optional.<String>absent());

            HubwayStopLocation newStop = hubwayBuilder.build();
            newStop.addRoute(routeConfig.getRouteName());
            builder.put(data.tag, newStop);
        }

        ImmutableMap<String, StopLocation> stopsToReplace = builder.build();
        locations.replaceStops(routeConfig.getRouteName(), stopsToReplace);
    }

	public List<PredictionStopLocationPair> getPairs() {
        List<PredictionStopLocationPair> pairs = Lists.newArrayList();

        for (HubwayStopData data : hubwayStopData) {
            StopLocation stop = routeConfig.getStop(data.tag);

            if (stop != null && data.name.equals(stop.getTitle())) {
                String text = makeText(data.numberBikes, data.numberEmptyDocks, data.locked, data.installed);
                SimplePrediction prediction = new SimplePrediction(routeConfig.getRouteName(),
                        routeConfig.getRouteTitle(), text);


                PredictionStopLocationPair pair = new PredictionStopLocationPair(prediction, stop);
                pairs.add(pair);
            }
        }

        return pairs;
    }

	private static String makeText(String numberBikes, String numberEmptyDocks, boolean locked, boolean installed) {
		StringBuilder ret = new StringBuilder();

		ret.append("Bikes: ").append(numberBikes).append("<br />");
		ret.append("Empty Docks: ").append(numberEmptyDocks).append("<br />");
		if (locked) {
			ret.append("<b>Locked</b><br />");
		}
		if (!installed) {
			ret.append("<b>Not installed</b><br />");
		}

		return ret.toString();
	}
}
