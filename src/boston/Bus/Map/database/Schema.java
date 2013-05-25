package boston.Bus.Map.database;

import android.database.DatabaseUtils.InsertHelper;

import java.util.Collection;

public class Schema {
    public static final String dbName = "transitData";
    public static final String oldDb = "bostonBusMap";

    private static final int INT_TRUE = 1;
    private static final int INT_FALSE = 0;

    public static int toInteger(boolean b) {
        return b ? INT_TRUE : INT_FALSE;
    }

    public static boolean fromInteger(int i) {
        return i == INT_TRUE;
    }


    public static class Alerts {
        public static final String table = "alerts"; 
        public static final String[] columns = new String[] {
            "route", "alertindex"
        };

        public static final int routeIndex = 1;
        public static final String routeColumn = "route";
        public static final String routeColumnOnTable = "alerts.route";
        public static final int alertindexIndex = 2;
        public static final String alertindexColumn = "alertindex";
        public static final String alertindexColumnOnTable = "alerts.alertindex";

        public static final String dropSql = "DROP TABLE IF EXISTS alerts";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS alerts (route STRING, alertindex INTEGER PRIMARY KEY)";
        public static class Bean {
            public final String route;
            public final int alertindex;
            public Bean(String route, int alertindex) {
                this.route = route;
                this.alertindex = alertindex;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.route, bean.alertindex);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String route, int alertindex) {
            helper.prepareForReplace();
            helper.bind(routeIndex, route);
            helper.bind(alertindexIndex, alertindex);
            helper.execute();
        }
    }
    public static class Arrivals {
        public static final String table = "arrivals"; 
        public static final String[] columns = new String[] {
            "id", "blob"
        };

        public static final int idIndex = 1;
        public static final String idColumn = "id";
        public static final String idColumnOnTable = "arrivals.id";
        public static final int blobIndex = 2;
        public static final String blobColumn = "blob";
        public static final String blobColumnOnTable = "arrivals.blob";

        public static final String dropSql = "DROP TABLE IF EXISTS arrivals";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS arrivals (id INTEGER PRIMARY KEY, blob BLOB)";
        public static class Bean {
            public final int id;
            public final byte[] blob;
            public Bean(int id, byte[] blob) {
                this.id = id;
                this.blob = blob;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.id, bean.blob);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, int id, byte[] blob) {
            helper.prepareForReplace();
            helper.bind(idIndex, id);
            helper.bind(blobIndex, blob);
            helper.execute();
        }
    }
    public static class Bounds {
        public static final String table = "bounds"; 
        public static final String[] columns = new String[] {
            "route", "weekdays", "start", "stop"
        };

        public static final int routeIndex = 1;
        public static final String routeColumn = "route";
        public static final String routeColumnOnTable = "bounds.route";
        public static final int weekdaysIndex = 2;
        public static final String weekdaysColumn = "weekdays";
        public static final String weekdaysColumnOnTable = "bounds.weekdays";
        public static final int startIndex = 3;
        public static final String startColumn = "start";
        public static final String startColumnOnTable = "bounds.start";
        public static final int stopIndex = 4;
        public static final String stopColumn = "stop";
        public static final String stopColumnOnTable = "bounds.stop";

        public static final String dropSql = "DROP TABLE IF EXISTS bounds";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS bounds (route STRING, weekdays INTEGER, start INTEGER, stop INTEGER)";
        public static class Bean {
            public final String route;
            public final int weekdays;
            public final int start;
            public final int stop;
            public Bean(String route, int weekdays, int start, int stop) {
                this.route = route;
                this.weekdays = weekdays;
                this.start = start;
                this.stop = stop;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.route, bean.weekdays, bean.start, bean.stop);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String route, int weekdays, int start, int stop) {
            helper.prepareForReplace();
            helper.bind(routeIndex, route);
            helper.bind(weekdaysIndex, weekdays);
            helper.bind(startIndex, start);
            helper.bind(stopIndex, stop);
            helper.execute();
        }
    }
    public static class Directions {
        public static final String table = "directions"; 
        public static final String[] columns = new String[] {
            "dirTag", "dirNameKey", "dirTitleKey", "dirRouteKey", "useAsUI"
        };

        public static final int dirTagIndex = 1;
        public static final String dirTagColumn = "dirTag";
        public static final String dirTagColumnOnTable = "directions.dirTag";
        public static final int dirNameKeyIndex = 2;
        public static final String dirNameKeyColumn = "dirNameKey";
        public static final String dirNameKeyColumnOnTable = "directions.dirNameKey";
        public static final int dirTitleKeyIndex = 3;
        public static final String dirTitleKeyColumn = "dirTitleKey";
        public static final String dirTitleKeyColumnOnTable = "directions.dirTitleKey";
        public static final int dirRouteKeyIndex = 4;
        public static final String dirRouteKeyColumn = "dirRouteKey";
        public static final String dirRouteKeyColumnOnTable = "directions.dirRouteKey";
        public static final int useAsUIIndex = 5;
        public static final String useAsUIColumn = "useAsUI";
        public static final String useAsUIColumnOnTable = "directions.useAsUI";

        public static final String dropSql = "DROP TABLE IF EXISTS directions";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS directions (dirTag STRING PRIMARY KEY, dirNameKey STRING, dirTitleKey STRING, dirRouteKey STRING, useAsUI INTEGER)";
        public static class Bean {
            public final String dirTag;
            public final String dirNameKey;
            public final String dirTitleKey;
            public final String dirRouteKey;
            public final int useAsUI;
            public Bean(String dirTag, String dirNameKey, String dirTitleKey, String dirRouteKey, int useAsUI) {
                this.dirTag = dirTag;
                this.dirNameKey = dirNameKey;
                this.dirTitleKey = dirTitleKey;
                this.dirRouteKey = dirRouteKey;
                this.useAsUI = useAsUI;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.dirTag, bean.dirNameKey, bean.dirTitleKey, bean.dirRouteKey, bean.useAsUI);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String dirTag, String dirNameKey, String dirTitleKey, String dirRouteKey, int useAsUI) {
            helper.prepareForReplace();
            helper.bind(dirTagIndex, dirTag);
            helper.bind(dirNameKeyIndex, dirNameKey);
            helper.bind(dirTitleKeyIndex, dirTitleKey);
            helper.bind(dirRouteKeyIndex, dirRouteKey);
            helper.bind(useAsUIIndex, useAsUI);
            helper.execute();
        }
    }
    public static class DirectionsStops {
        public static final String table = "directionsStops"; 
        public static final String[] columns = new String[] {
            "dirTag", "tag"
        };

        public static final int dirTagIndex = 1;
        public static final String dirTagColumn = "dirTag";
        public static final String dirTagColumnOnTable = "directionsStops.dirTag";
        public static final int tagIndex = 2;
        public static final String tagColumn = "tag";
        public static final String tagColumnOnTable = "directionsStops.tag";

        public static final String dropSql = "DROP TABLE IF EXISTS directionsStops";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS directionsStops (dirTag STRING, tag STRING)";
        public static class Bean {
            public final String dirTag;
            public final String tag;
            public Bean(String dirTag, String tag) {
                this.dirTag = dirTag;
                this.tag = tag;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.dirTag, bean.tag);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String dirTag, String tag) {
            helper.prepareForReplace();
            helper.bind(dirTagIndex, dirTag);
            helper.bind(tagIndex, tag);
            helper.execute();
        }
    }
    public static class Favorites {
        public static final String table = "favorites"; 
        public static final String[] columns = new String[] {
            "tag"
        };

        public static final int tagIndex = 1;
        public static final String tagColumn = "tag";
        public static final String tagColumnOnTable = "favorites.tag";

        public static final String dropSql = "DROP TABLE IF EXISTS favorites";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS favorites (tag STRING PRIMARY KEY)";
        public static class Bean {
            public final String tag;
            public Bean(String tag) {
                this.tag = tag;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.tag);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String tag) {
            helper.prepareForReplace();
            helper.bind(tagIndex, tag);
            helper.execute();
        }
    }
    public static class Locations {
        public static final String table = "locations"; 
        public static final String[] columns = new String[] {
            "lat", "lon", "name"
        };

        public static final int latIndex = 1;
        public static final String latColumn = "lat";
        public static final String latColumnOnTable = "locations.lat";
        public static final int lonIndex = 2;
        public static final String lonColumn = "lon";
        public static final String lonColumnOnTable = "locations.lon";
        public static final int nameIndex = 3;
        public static final String nameColumn = "name";
        public static final String nameColumnOnTable = "locations.name";

        public static final String dropSql = "DROP TABLE IF EXISTS locations";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS locations (lat FLOAT, lon FLOAT, name STRING PRIMARY KEY)";
        public static class Bean {
            public final float lat;
            public final float lon;
            public final String name;
            public Bean(float lat, float lon, String name) {
                this.lat = lat;
                this.lon = lon;
                this.name = name;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.lat, bean.lon, bean.name);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, float lat, float lon, String name) {
            helper.prepareForReplace();
            helper.bind(latIndex, lat);
            helper.bind(lonIndex, lon);
            helper.bind(nameIndex, name);
            helper.execute();
        }
    }
    public static class Routes {
        public static final String table = "routes"; 
        public static final String[] columns = new String[] {
            "route", "color", "oppositecolor", "pathblob", "listorder", "agencyid", "routetitle"
        };

        public static final int routeIndex = 1;
        public static final String routeColumn = "route";
        public static final String routeColumnOnTable = "routes.route";
        public static final int colorIndex = 2;
        public static final String colorColumn = "color";
        public static final String colorColumnOnTable = "routes.color";
        public static final int oppositecolorIndex = 3;
        public static final String oppositecolorColumn = "oppositecolor";
        public static final String oppositecolorColumnOnTable = "routes.oppositecolor";
        public static final int pathblobIndex = 4;
        public static final String pathblobColumn = "pathblob";
        public static final String pathblobColumnOnTable = "routes.pathblob";
        public static final int listorderIndex = 5;
        public static final String listorderColumn = "listorder";
        public static final String listorderColumnOnTable = "routes.listorder";
        public static final int agencyidIndex = 6;
        public static final String agencyidColumn = "agencyid";
        public static final String agencyidColumnOnTable = "routes.agencyid";

        public static final int enumagencyidCommuterRail = 1;
        public static final int enumagencyidSubway = 2;
        public static final int enumagencyidBus = 3;
        public static final int routetitleIndex = 7;
        public static final String routetitleColumn = "routetitle";
        public static final String routetitleColumnOnTable = "routes.routetitle";

        public static final String dropSql = "DROP TABLE IF EXISTS routes";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS routes (route STRING PRIMARY KEY, color INTEGER, oppositecolor INTEGER, pathblob BLOB, listorder INTEGER, agencyid INTEGER, routetitle STRING)";
        public static class Bean {
            public final String route;
            public final int color;
            public final int oppositecolor;
            public final byte[] pathblob;
            public final int listorder;
            public final int agencyid;
            public final String routetitle;
            public Bean(String route, int color, int oppositecolor, byte[] pathblob, int listorder, int agencyid, String routetitle) {
                this.route = route;
                this.color = color;
                this.oppositecolor = oppositecolor;
                this.pathblob = pathblob;
                this.listorder = listorder;
                this.agencyid = agencyid;
                this.routetitle = routetitle;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.route, bean.color, bean.oppositecolor, bean.pathblob, bean.listorder, bean.agencyid, bean.routetitle);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String route, int color, int oppositecolor, byte[] pathblob, int listorder, int agencyid, String routetitle) {
            helper.prepareForReplace();
            helper.bind(routeIndex, route);
            helper.bind(colorIndex, color);
            helper.bind(oppositecolorIndex, oppositecolor);
            helper.bind(pathblobIndex, pathblob);
            helper.bind(listorderIndex, listorder);
            helper.bind(agencyidIndex, agencyid);
            helper.bind(routetitleIndex, routetitle);
            helper.execute();
        }
    }
    public static class Stop_times {
        public static final String table = "stop_times"; 
        public static final String[] columns = new String[] {
            "trip_id", "arrival_id", "stop_list_id", "offset"
        };

        public static final int trip_idIndex = 1;
        public static final String trip_idColumn = "trip_id";
        public static final String trip_idColumnOnTable = "stop_times.trip_id";
        public static final int arrival_idIndex = 2;
        public static final String arrival_idColumn = "arrival_id";
        public static final String arrival_idColumnOnTable = "stop_times.arrival_id";
        public static final int stop_list_idIndex = 3;
        public static final String stop_list_idColumn = "stop_list_id";
        public static final String stop_list_idColumnOnTable = "stop_times.stop_list_id";
        public static final int offsetIndex = 4;
        public static final String offsetColumn = "offset";
        public static final String offsetColumnOnTable = "stop_times.offset";

        public static final String dropSql = "DROP TABLE IF EXISTS stop_times";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS stop_times (trip_id INTEGER, arrival_id INTEGER, stop_list_id INTEGER, offset INTEGER)";
        public static class Bean {
            public final int trip_id;
            public final int arrival_id;
            public final int stop_list_id;
            public final int offset;
            public Bean(int trip_id, int arrival_id, int stop_list_id, int offset) {
                this.trip_id = trip_id;
                this.arrival_id = arrival_id;
                this.stop_list_id = stop_list_id;
                this.offset = offset;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.trip_id, bean.arrival_id, bean.stop_list_id, bean.offset);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, int trip_id, int arrival_id, int stop_list_id, int offset) {
            helper.prepareForReplace();
            helper.bind(trip_idIndex, trip_id);
            helper.bind(arrival_idIndex, arrival_id);
            helper.bind(stop_list_idIndex, stop_list_id);
            helper.bind(offsetIndex, offset);
            helper.execute();
        }
    }
    public static class Stopmapping {
        public static final String table = "stopmapping"; 
        public static final String[] columns = new String[] {
            "route", "tag", "dirTag"
        };

        public static final int routeIndex = 1;
        public static final String routeColumn = "route";
        public static final String routeColumnOnTable = "stopmapping.route";
        public static final int tagIndex = 2;
        public static final String tagColumn = "tag";
        public static final String tagColumnOnTable = "stopmapping.tag";
        public static final int dirTagIndex = 3;
        public static final String dirTagColumn = "dirTag";
        public static final String dirTagColumnOnTable = "stopmapping.dirTag";

        public static final String dropSql = "DROP TABLE IF EXISTS stopmapping";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS stopmapping (route STRING, tag STRING, dirTag STRING, PRIMARY KEY (route, tag))";
        public static class Bean {
            public final String route;
            public final String tag;
            public final String dirTag;
            public Bean(String route, String tag, String dirTag) {
                this.route = route;
                this.tag = tag;
                this.dirTag = dirTag;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.route, bean.tag, bean.dirTag);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String route, String tag, String dirTag) {
            helper.prepareForReplace();
            helper.bind(routeIndex, route);
            helper.bind(tagIndex, tag);
            helper.bind(dirTagIndex, dirTag);
            helper.execute();
        }
    }
    public static class Stops {
        public static final String table = "stops"; 
        public static final String[] columns = new String[] {
            "tag", "lat", "lon", "title"
        };

        public static final int tagIndex = 1;
        public static final String tagColumn = "tag";
        public static final String tagColumnOnTable = "stops.tag";
        public static final int latIndex = 2;
        public static final String latColumn = "lat";
        public static final String latColumnOnTable = "stops.lat";
        public static final int lonIndex = 3;
        public static final String lonColumn = "lon";
        public static final String lonColumnOnTable = "stops.lon";
        public static final int titleIndex = 4;
        public static final String titleColumn = "title";
        public static final String titleColumnOnTable = "stops.title";

        public static final String dropSql = "DROP TABLE IF EXISTS stops";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS stops (tag STRING PRIMARY KEY, lat FLOAT, lon FLOAT, title STRING)";
        public static class Bean {
            public final String tag;
            public final float lat;
            public final float lon;
            public final String title;
            public Bean(String tag, float lat, float lon, String title) {
                this.tag = tag;
                this.lat = lat;
                this.lon = lon;
                this.title = title;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.tag, bean.lat, bean.lon, bean.title);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String tag, float lat, float lon, String title) {
            helper.prepareForReplace();
            helper.bind(tagIndex, tag);
            helper.bind(latIndex, lat);
            helper.bind(lonIndex, lon);
            helper.bind(titleIndex, title);
            helper.execute();
        }
    }
    public static class Subway {
        public static final String table = "subway"; 
        public static final String[] columns = new String[] {
            "tag", "platformorder", "branch"
        };

        public static final int tagIndex = 1;
        public static final String tagColumn = "tag";
        public static final String tagColumnOnTable = "subway.tag";
        public static final int platformorderIndex = 2;
        public static final String platformorderColumn = "platformorder";
        public static final String platformorderColumnOnTable = "subway.platformorder";
        public static final int branchIndex = 3;
        public static final String branchColumn = "branch";
        public static final String branchColumnOnTable = "subway.branch";

        public static final String dropSql = "DROP TABLE IF EXISTS subway";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS subway (tag STRING PRIMARY KEY, platformorder INTEGER, branch STRING)";
        public static class Bean {
            public final String tag;
            public final int platformorder;
            public final String branch;
            public Bean(String tag, int platformorder, String branch) {
                this.tag = tag;
                this.platformorder = platformorder;
                this.branch = branch;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.tag, bean.platformorder, bean.branch);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, String tag, int platformorder, String branch) {
            helper.prepareForReplace();
            helper.bind(tagIndex, tag);
            helper.bind(platformorderIndex, platformorder);
            helper.bind(branchIndex, branch);
            helper.execute();
        }
    }
    public static class Trip_ids {
        public static final String table = "trip_ids"; 
        public static final String[] columns = new String[] {
            "id", "trip_id", "route_id", "dir_tag"
        };

        public static final int idIndex = 1;
        public static final String idColumn = "id";
        public static final String idColumnOnTable = "trip_ids.id";
        public static final int trip_idIndex = 2;
        public static final String trip_idColumn = "trip_id";
        public static final String trip_idColumnOnTable = "trip_ids.trip_id";
        public static final int route_idIndex = 3;
        public static final String route_idColumn = "route_id";
        public static final String route_idColumnOnTable = "trip_ids.route_id";
        public static final int dir_tagIndex = 4;
        public static final String dir_tagColumn = "dir_tag";
        public static final String dir_tagColumnOnTable = "trip_ids.dir_tag";

        public static final String dropSql = "DROP TABLE IF EXISTS trip_ids";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS trip_ids (id INTEGER PRIMARY KEY, trip_id STRING, route_id STRING, dir_tag STRING)";
        public static class Bean {
            public final int id;
            public final String trip_id;
            public final String route_id;
            public final String dir_tag;
            public Bean(int id, String trip_id, String route_id, String dir_tag) {
                this.id = id;
                this.trip_id = trip_id;
                this.route_id = route_id;
                this.dir_tag = dir_tag;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.id, bean.trip_id, bean.route_id, bean.dir_tag);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, int id, String trip_id, String route_id, String dir_tag) {
            helper.prepareForReplace();
            helper.bind(idIndex, id);
            helper.bind(trip_idIndex, trip_id);
            helper.bind(route_idIndex, route_id);
            helper.bind(dir_tagIndex, dir_tag);
            helper.execute();
        }
    }
    public static class Trip_stops {
        public static final String table = "trip_stops"; 
        public static final String[] columns = new String[] {
            "id", "blob"
        };

        public static final int idIndex = 1;
        public static final String idColumn = "id";
        public static final String idColumnOnTable = "trip_stops.id";
        public static final int blobIndex = 2;
        public static final String blobColumn = "blob";
        public static final String blobColumnOnTable = "trip_stops.blob";

        public static final String dropSql = "DROP TABLE IF EXISTS trip_stops";
        public static final String createSql = "CREATE TABLE IF NOT EXISTS trip_stops (id INTEGER PRIMARY KEY, blob BLOB)";
        public static class Bean {
            public final int id;
            public final byte[] blob;
            public Bean(int id, byte[] blob) {
                this.id = id;
                this.blob = blob;
            }
        }
        public static void executeInsertHelper(InsertHelper helper, Collection<Bean> beans) {
            for (Bean bean : beans) {
                executeInsertHelper(helper, bean.id, bean.blob);
            }
        }
        public static void executeInsertHelper(InsertHelper helper, int id, byte[] blob) {
            helper.prepareForReplace();
            helper.bind(idIndex, id);
            helper.bind(blobIndex, blob);
            helper.execute();
        }
    }
}
