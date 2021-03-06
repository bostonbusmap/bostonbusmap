package com.schneeloch.bostonbusmap_library.data;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by george on 2/21/15.
 */
public class TransitSourceCache {
    private static final long fetchDelay = 15000;

    protected long lastPredictionWholeRefresh;
    protected long lastVehicleWholeRefresh;
    /**
     * route id -> stop id -> time in millis
     */
    private final ConcurrentHashMap<RouteStopPair, Long> predictionsLastUpdatesByStop;
    /**
     * route id -> time in millis
     */
    private final ConcurrentHashMap<String, Long> predictionsLastUpdatesByRoute;
    /**
     * route id -> time in millis
     */
    private final ConcurrentHashMap<String, Long> vehiclesLastUpdatesByRoute;

    public TransitSourceCache() {
        lastPredictionWholeRefresh = 0;
        lastVehicleWholeRefresh = 0;
        predictionsLastUpdatesByRoute = new ConcurrentHashMap<>();
        predictionsLastUpdatesByStop = new ConcurrentHashMap<>();
        vehiclesLastUpdatesByRoute = new ConcurrentHashMap<>();
    }

    public void updateVehiclesForRoute(String route) {
        vehiclesLastUpdatesByRoute.put(route, System.currentTimeMillis());
    }

    public boolean canUpdateVehiclesForRoute(String route) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = vehiclesLastUpdatesByRoute.get(route);
        if (lastUpdate == null) {
            return true;
        }
        else {
            long lastUpdateLong = lastUpdate;
            return currentTime > lastUpdateLong + fetchDelay;
        }
    }

    public void updateAllVehicles() {
        lastVehicleWholeRefresh = System.currentTimeMillis();
        vehiclesLastUpdatesByRoute.clear();
    }

    public boolean canUpdateAllVehicles() {
        return System.currentTimeMillis() > lastVehicleWholeRefresh + fetchDelay;
    }

    public void updatePredictionForStop(RouteStopPair pair) {
        predictionsLastUpdatesByStop.put(pair, System.currentTimeMillis());
    }

    public boolean canUpdatePredictionForStop(RouteStopPair pair) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = predictionsLastUpdatesByStop.get(pair);
        if (lastUpdate == null) {
            return true;
        }
        else {
            long lastUpdateLong = lastUpdate;
            return currentTime > lastUpdateLong + fetchDelay;
        }
    }

    public void updateAllPredictions() {
        predictionsLastUpdatesByRoute.clear();
        predictionsLastUpdatesByStop.clear();
        lastPredictionWholeRefresh = System.currentTimeMillis();
    }

    public boolean canUpdateAllPredictions() {
        long currentTime = System.currentTimeMillis();
        return currentTime > lastPredictionWholeRefresh + fetchDelay;
    }

    public boolean canUpdatePredictionForRoute(String routeName) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = predictionsLastUpdatesByRoute.get(routeName);
        if (lastUpdate == null) {
            return true;
        }
        else {
            long lastUpdateLong = lastUpdate;
            return currentTime > lastUpdateLong + fetchDelay;
        }
    }

    public void updatePredictionForRoute(String route) {
        long currentMillis = System.currentTimeMillis();
        predictionsLastUpdatesByRoute.put(route, currentMillis);

        // TODO: we should probably update stops with the same route
        // here, but no transit source uses both updatePredictionForRoute
        // and updatePredictionForStops
    }
}
