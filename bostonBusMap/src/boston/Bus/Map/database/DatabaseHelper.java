package boston.Bus.Map.database;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import boston.Bus.Map.data.Path;

import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.SubwayStopLocation;
import boston.Bus.Map.main.UpdateAsyncTask;
import boston.Bus.Map.transit.TransitSource;
import boston.Bus.Map.transit.TransitSystem;
import boston.Bus.Map.ui.ProgressMessage;
import boston.Bus.Map.util.Box;
import boston.Bus.Map.util.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.os.Debug;
import android.os.Parcel;
import android.os.StatFs;
import android.util.Log;

/**
 * Handles the database which stores route information
 * 
 * @author schneg
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
	private final static String dbName = "bostonBusMap";
	 
	private final static String verboseRoutes = "routes";
	private final static String verboseStops = "stops";
	private final static String stopsRoutesMap = "stopmapping";
	private final static String stopsRoutesMapIndexTag = "IDX_stopmapping";
	private final static String stopsRoutesMapIndexRoute = "IDX_routemapping";
	private final static String subwaySpecificTable = "subway";
	
	
	private final static String directionsTable = "directions";
	private final static String stopsTable = "stops";
	private final static String routesTable = "routes";
	private final static String pathsTable = "paths";
	private final static String blobsTable = "blobs";
	private final static String oldFavoritesTable = "favs";
	private final static String newFavoritesTable = "favs2";
	private final static String routePoolTable = "routepool";
	
	private final static String verboseFavorites = "favorites";
	
	private final static String routeKey = "route";
	private final static String stopIdKey = "stopId";
	private final static String newFavoritesTagKey = "tag";
	private final static String latitudeKey = "lat";
	private final static String longitudeKey = "lon";
	private final static String titleKey = "title";
	private final static String dirtagKey = "dirtag";
	private final static String nameKey = "name";
	private final static String pathIdKey = "pathid";
	private final static String blobKey = "blob";
	private final static String oldFavoritesIdKey = "idkey";
	private final static String newFavoritesRouteKey = "route";

	private final static String colorKey = "color";
	private final static String oppositeColorKey = "oppositecolor";
	private final static String pathsBlobKey = "pathblob";
	private final static String stopTagKey = "tag";
	private final static String branchKey = "branch";
	private final static String stopTitleKey = "title";
	private final static String platformOrderKey = "platformorder";
	
	
	private static final String dirTagKey = "dirTag";
	private static final String dirNameKey = "dirNameKey";
	private static final String dirTitleKey = "dirTitleKey";
	private static final String dirRouteKey = "dirRouteKey";
	
	private final static int tagIndex = 1;
	private final static int nameIndex = 2;
	private final static int titleIndex = 3;
	
	/**
	 * The first version where we serialize as bytes, not necessarily the first db version
	 */
	public final static int FIRST_DB_VERSION = 5;
	public final static int ADDED_FAVORITE_DB_VERSION = 6;
	public final static int NEW_ROUTES_DB_VERSION = 7;	
	public final static int ROUTE_POOL_DB_VERSION = 8;
	public final static int STOP_LOCATIONS_STORE_ROUTE_STRINGS = 9;
	public final static int STOP_LOCATIONS_ADD_DIRECTIONS = 10;
	public final static int SUBWAY_VERSION = 11;
	public final static int ADDED_PLATFORM_ORDER = 12;
	public final static int VERBOSE_DB = 13;
	
	
	public final static int CURRENT_DB_VERSION = VERBOSE_DB;
	
	public DatabaseHelper(Context context) {
		super(context, dbName, null, CURRENT_DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		/*db.execSQL("CREATE TABLE IF NOT EXISTS " + blobsTable + " (" + routeKey + " STRING PRIMARY KEY, " + blobKey + " BLOB)");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + routePoolTable + " (" + routeKey + " STRING PRIMARY KEY)");*/
		db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseFavorites + " (" + latitudeKey + " FLOAT, " + longitudeKey + " FLOAT" +
				", PRIMARY KEY (" + latitudeKey + ", " + longitudeKey + "))");
						
		db.execSQL("CREATE TABLE IF NOT EXISTS " + directionsTable + " (" + dirTagKey + " STRING PRIMARY KEY, " + 
				dirNameKey + " STRING, " + dirTitleKey + " STRING, " + dirRouteKey + " STRING)");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseRoutes + " (" + routeKey + " STRING PRIMARY KEY, " + colorKey + 
				" INTEGER, " + oppositeColorKey + " INTEGER, " + pathsBlobKey + " BLOB)");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseStops + " (" + stopTagKey + " STRING PRIMARY KEY, " + 
				latitudeKey + " FLOAT, " + longitudeKey + " FLOAT, " + stopTitleKey + " STRING)");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + stopsRoutesMap + " (" + routeKey + " STRING, " + stopTagKey + " STRING, " +
				dirTagKey + " STRING, PRIMARY KEY (" + routeKey + ", " + stopTagKey + "))");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + subwaySpecificTable + " (" + stopTagKey + " STRING PRIMARY KEY, " +
				platformOrderKey + " INTEGER, " + 
				branchKey + " STRING)");
		db.execSQL("CREATE INDEX IF NOT EXISTS " + stopsRoutesMapIndexRoute + " ON " + stopsRoutesMap + " (" + routeKey + ")");
		db.execSQL("CREATE INDEX IF NOT EXISTS " + stopsRoutesMapIndexTag + " ON " + stopsRoutesMap + " (" + stopTagKey + ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v("BostonBusMap", "upgrading database from " + oldVersion + " to " + newVersion);
		ArrayList<FloatPoint> favorites = null;
		if (oldVersion < VERBOSE_DB)
		{
			favorites = readOldFavorites(db);
		}

		db.beginTransaction();
		db.execSQL("DROP TABLE IF EXISTS " + directionsTable);
		db.execSQL("DROP TABLE IF EXISTS " + stopsTable);
		db.execSQL("DROP TABLE IF EXISTS " + routesTable);
		db.execSQL("DROP TABLE IF EXISTS " + pathsTable);
		db.execSQL("DROP TABLE IF EXISTS " + blobsTable);
		db.execSQL("DROP TABLE IF EXISTS " + verboseRoutes);
		db.execSQL("DROP TABLE IF EXISTS " + verboseStops);
		db.execSQL("DROP TABLE IF EXISTS " + stopsRoutesMap);

		db.execSQL("DROP TABLE IF EXISTS " + oldFavoritesTable);
		db.execSQL("DROP TABLE IF EXISTS " + newFavoritesTable);
		//if it's verboseFavorites, we want to save it since it's user specified data

		onCreate(db);

		if (favorites != null)
		{
			writeVerboseFavorites(db, favorites);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		
	}

	private void writeVerboseFavorites(SQLiteDatabase db, ArrayList<FloatPoint> favorites) {
		for (FloatPoint favorite : favorites) {
			ContentValues values = new ContentValues();
			values.put(latitudeKey, favorite.lat);
			values.put(longitudeKey, favorite.lon);
			
			db.replace(verboseFavorites, null, values);
		}
	}
	
	private ArrayList<FloatPoint> readOldFavorites(SQLiteDatabase database)
	{
		ArrayList<FloatPoint> ret = new ArrayList<FloatPoint>();
		Cursor cursor = null;
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(verboseFavorites + ", " + verboseStops);
		builder.setDistinct(true);
		
		try
		{
			cursor = builder.query(database, new String[] {
					verboseStops + "." + latitudeKey, verboseStops + "." + longitudeKey},
					verboseStops + "." + stopTagKey + "=" + verboseFavorites + "." + stopTagKey,
					null, null, null, null);
		
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false)
			{
				FloatPoint point = new FloatPoint(cursor.getFloat(0), cursor.getFloat(1));
				
				ret.add(point);
				cursor.moveToNext();
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
		
		return ret;
	}
	
	/**
	 * Fill the given HashSet with all stop tags that are favorites
	 * @param favorites
	 */
	public synchronized void populateFavorites(HashSet<String> favorites)
	{
		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor = null;

		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(verboseFavorites + ", " + verboseStops);
		builder.setDistinct(true);
		try
		{
			cursor = builder.query(database, new String[]{stopTagKey}, 
					verboseStops + "." + latitudeKey + "=" + verboseFavorites + "." + latitudeKey + " AND " + 
					verboseStops + "." + longitudeKey + "=" + verboseFavorites + "." + longitudeKey,
					null, null, null, null);
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false)
			{
				String favoriteStopKey = cursor.getString(0);
				
				favorites.add(favoriteStopKey);

				cursor.moveToNext();
			}
		}		
		finally
		{
			database.close();
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}
	
	public synchronized void saveMapping(HashMap<String, RouteConfig> mapping,
			boolean wipe, HashSet<String> sharedStops, UpdateAsyncTask task) throws IOException
	{
		SQLiteDatabase database = getWritableDatabase();
		try
		{
			database.beginTransaction();

			if (wipe)
			{
				//database.delete(stopsTable, null, null);
				//database.delete(directionsTable, null, null);
				//database.delete(pathsTable, null, null);
				//database.delete(blobsTable, null, null);
				
				database.delete(verboseStops, null, null);
				database.delete(verboseRoutes, null, null);
			}

			int total = mapping.keySet().size();
			task.publish(new ProgressMessage(ProgressMessage.SET_MAX, total));
			
			int count = 0;
			for (String route : mapping.keySet())
			{
				RouteConfig routeConfig = mapping.get(route);
				if (routeConfig != null)
				{
					saveMappingKernel(database, route, routeConfig, sharedStops);
				}
				
				count++;
				task.publish(count);
			}

			database.setTransactionSuccessful();
			database.endTransaction();

		}
		finally
		{
			database.close();
		}
	}
	
	/**
	 * 
	 * @param database
	 * @param route
	 * @param routeConfig
	 * @param useInsert insert all rows, don't replace them. I assume this is faster since there's no lookup involved
	 * @throws IOException 
	 */
	private void saveMappingKernel(SQLiteDatabase database, String route, RouteConfig routeConfig,
			HashSet<String> sharedStops) throws IOException
	{
		Box serializedPath = new Box(null, CURRENT_DB_VERSION);
		
		routeConfig.serializePath(serializedPath);
		
		byte[] serializedPathBlob = serializedPath.getBlob();
		
		{
			ContentValues values = new ContentValues();
			values.put(routeKey, route);
			values.put(pathsBlobKey, serializedPathBlob);
			values.put(colorKey, routeConfig.getColor());
			values.put(oppositeColorKey, routeConfig.getOppositeColor());

			database.replace(verboseRoutes, null, values);
		}
		
		//add all stops associated with the route, if they don't already exist
		

		database.delete(stopsRoutesMap, routeKey + "=?", new String[]{route});
		
		
		for (StopLocation stop : routeConfig.getStops())
		{
			/*"CREATE TABLE IF NOT EXISTS " + verboseStops + " (" + stopTagKey + " STRING PRIMARY KEY, " + 
			latitudeKey + " FLOAT, " + longitudeKey + " FLOAT, " + stopTitleKey + " STRING, " +
			branchKey + " STRING, " + platformOrderKey + " SHORT)"*/
			String stopTag = stop.getStopTag();
			
			if (sharedStops.contains(stopTag) == false)
			{
			
				sharedStops.add(stopTag);

				{
					ContentValues values = new ContentValues();
					values.put(stopTagKey, stopTag);
					values.put(latitudeKey, stop.getLatitudeAsDegrees());
					values.put(longitudeKey, stop.getLongitudeAsDegrees());
					values.put(stopTitleKey, stop.getTitle());

					database.insert(verboseStops, null, values);
				}

				if (stop instanceof SubwayStopLocation)
				{
					SubwayStopLocation subwayStop = (SubwayStopLocation)stop;
					ContentValues values = new ContentValues();
					values.put(stopTagKey, stopTag);
					values.put(platformOrderKey, subwayStop.getPlatformOrder());
					values.put(branchKey, subwayStop.getBranch());

					database.insert(subwaySpecificTable, null, values);
				}
			}
			
			{
				//show that there's a relationship between the stop and this route
				ContentValues values = new ContentValues();
				values.put(routeKey, route);
				values.put(stopTagKey, stopTag);
				values.put(dirtagKey, stop.getDirTagForRoute(route));
				database.insert(stopsRoutesMap, null, values);
			}
		}
	}

	public synchronized boolean checkFreeSpace() {
		SQLiteDatabase database = getReadableDatabase();
		try
		{
			String path = database.getPath();
			
			StatFs statFs = new StatFs(path);
			long freeSpace = (long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize(); 
		
			Log.v("BostonBusMap", "free database space: " + freeSpace);
			return freeSpace >= 1024 * 1024 * 4;
		}
		catch (Exception e)
		{
			//if for some reason we don't have permission to check free space available, just hope that everything's ok
			return true;
		}
		finally
		{
			database.close();
		}
	}

	public synchronized boolean saveFavorite(float lat, float lon, boolean isFavorite) {
		Log.v("BostonBusMap", "Saving favorite " + lat + ", " + lon + " as " + isFavorite);
		SQLiteDatabase database = getWritableDatabase();
		try
		{
			if (database.isOpen() == false)
			{
				Log.e("BostonBusMap", "SERIOUS ERROR: database didn't save data properly");
				return false;
			}
			database.beginTransaction();

			if (isFavorite)
			{
				ContentValues values = new ContentValues();
				values.put(latitudeKey, lat);
				values.put(longitudeKey, lon);
				database.replace(verboseFavorites, null, values);
			}
			else
			{
				database.delete(verboseFavorites, latitudeKey + "=? AND " + longitudeKey + "=?",
						new String[] {((Float)lat).toString(), ((Float)lon).toString()});					
			}

			database.setTransactionSuccessful();
			database.endTransaction();

		}
		finally
		{
			database.close();
		}
		return true;
	}

	public synchronized RouteConfig getRoute(String routeToUpdate, HashMap<String, StopLocation> sharedStops,
			TransitSystem transitSystem) throws IOException {
		SQLiteDatabase database = getReadableDatabase();
		Cursor routeCursor = null;
		Cursor stopCursor = null;
		try
		{
			/*db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseRoutes + " (" + routeKey + " STRING PRIMARY KEY, " + colorKey + 
					" INTEGER, " + oppositeColorKey + " INTEGER, " + pathsBlobKey + " BLOB)");*/

			//get the route-specific information, like the path outline and the color
			routeCursor = database.query(verboseRoutes, new String[]{colorKey, oppositeColorKey, pathsBlobKey}, routeKey + "=?",
					new String[]{routeToUpdate}, null, null, null);
			if (routeCursor.getCount() == 0)
			{
				return null;
			}
			
			routeCursor.moveToFirst();

			TransitSource source = transitSystem.getTransitSource(routeToUpdate);

			int color = routeCursor.getInt(0);
			int oppositeColor = routeCursor.getInt(1);
			byte[] pathsBlob = routeCursor.getBlob(2);
			Box pathsBlobBox = new Box(pathsBlob, CURRENT_DB_VERSION);

			RouteConfig routeConfig = new RouteConfig(routeToUpdate, color, oppositeColor, source, pathsBlobBox);

			
			
			//get all stops, joining in the subway stops, making sure that the stop references the route we're on
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
			String tables = verboseStops +
			" JOIN " + stopsRoutesMap + " AS sm1 ON (" + verboseStops + "." + stopTagKey + " = sm1." + stopTagKey + ")" +
			" JOIN " + stopsRoutesMap + " AS sm2 ON (" + verboseStops + "." + stopTagKey + " = sm2." + stopTagKey + ")" +
			" LEFT OUTER JOIN " + subwaySpecificTable + " ON (" + verboseStops + "." + stopTagKey + " = " + 
			subwaySpecificTable + "." + stopTagKey + ")";

			
			/* select stops.tag, lat, lon, title, platformorder, branch, stopmapping1.dirTag, stopmapping2.route 
			 * from stops inner join stopmapping as stopmapping1 on (stops.tag = stopmapping1.tag) 
			 * inner join stopmapping as stopmapping2 on (stops.tag = stopmapping2.tag)
			 * left outer join subway on (stops.tag = subway.tag) 
			 * where stopmapping1.route=71;*/ 
			builder.setTables(tables);
			
			String[] projectionIn = new String[] {verboseStops + "." + stopTagKey, latitudeKey, longitudeKey, 
					stopTitleKey, platformOrderKey, branchKey, "sm1." + dirTagKey, "sm2." + routeKey};
			String select = "sm1." + routeKey + "=?";
			String[] selectArray = new String[]{routeToUpdate};
			
			Log.v("BostonBusMap", SQLiteQueryBuilder.buildQueryString(false, tables, projectionIn, "sm1." + routeKey + "=\"" + routeToUpdate + "\"",
					null, null, null, null));
			
			stopCursor = builder.query(database, projectionIn, select, selectArray, null, null, null);
			
			
			stopCursor.moveToFirst();
			while (stopCursor.isAfterLast() == false)
			{
				String stopTag = stopCursor.getString(0);
				String dirTag = stopCursor.getString(6);
				String route = stopCursor.getString(7);

				//we need to ensure this stop is in the sharedstops and the route
				StopLocation stop = sharedStops.get(stopTag);
				if (stop != null)
				{
					//make sure it exists in the route too
					StopLocation stopInRoute = routeConfig.getStop(stopTag);
					if (stopInRoute == null)
					{
						routeConfig.addStop(stopTag, stop);
					}
					
				}
				else
				{
					stop = routeConfig.getStop(stopTag);
					
					if (stop == null)
					{
						float latitude = stopCursor.getFloat(1);
						float longitude = stopCursor.getFloat(2);
						String stopTitle = stopCursor.getString(3);
						String branch = stopCursor.getString(5);

						int platformOrder = 0;

						Drawable busStop = source.getBusStopDrawable();
						if (stopCursor.isNull(4) == false)
						{
							//TODO: we should have a factory somewhere to abstract details away
							platformOrder = stopCursor.getInt(4);

							stop = new SubwayStopLocation(latitude, longitude, busStop,
									stopTag, stopTitle, platformOrder, branch);
						}
						else
						{
							stop = new StopLocation(latitude, longitude, busStop, stopTag, stopTitle);
						}

						routeConfig.addStop(stopTag, stop);
					}
					
					sharedStops.put(stopTag, stop);
				}
				stop.addRouteAndDirTag(route, dirTag);
				
				stopCursor.moveToNext();
			}
			Log.v("BostonBusMap", "getRoute ended successfully");
			
			return routeConfig;
		}
		finally
		{
			database.close();
			if (routeCursor != null)
			{
				routeCursor.close();
			}
			if (stopCursor != null)
			{
				stopCursor.close();
			}
		}
	}

	public synchronized ArrayList<String> routeInfoNeedsUpdating(String[] supportedRoutes) {
		HashSet<String> routesInDB = new HashSet<String>();
		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor = null;
		try
		{
			cursor = database.query(verboseRoutes, new String[]{routeKey}, null, null, null, null, null);
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false)
			{
				routesInDB.add(cursor.getString(0));
				
				cursor.moveToNext();
			}
		}
		finally
		{
			database.close();
			if (cursor != null)
			{
				cursor.close();
			}
		}
		
		ArrayList<String> routesThatNeedUpdating = new ArrayList<String>();
		
		for (String route : supportedRoutes)
		{
			if (routesInDB.contains(route) == false)
			{
				routesThatNeedUpdating.add(route);
			}
		}
		
		return routesThatNeedUpdating;
	}

	/**
	 * Populate directions from the database
	 * 
	 * NOTE: these data structures are assumed to be synchronized
	 * @param indexes
	 * @param names
	 * @param titles
	 */
	public synchronized void refreshDirections(HashMap<String, Integer> indexes,
			ArrayList<String> names, ArrayList<String> titles, ArrayList<String> routes) {
		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor = null;
		try
		{
			cursor = database.query(directionsTable, new String[]{dirTagKey, dirNameKey, dirTitleKey, dirRouteKey},
					null, null, null, null, null);
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false)
			{
				String dirTag = cursor.getString(0);
				String dirName = cursor.getString(1);
				String dirTitle = cursor.getString(2);
				String dirRoute = cursor.getString(3);
				
				indexes.put(dirTag, names.size());
				names.add(dirName);
				titles.add(dirTitle);
				routes.add(dirRoute);
				
				cursor.moveToNext();
			}
		}
		finally
		{
			database.close();
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	public synchronized void writeDirections(boolean wipe, HashMap<String, Integer> indexes,
			ArrayList<String> names, ArrayList<String> titles, ArrayList<String> routes) {
		SQLiteDatabase database = getWritableDatabase();
		try
		{
			database.beginTransaction();
			if (wipe)
			{
				database.delete(directionsTable, null, null);
			}

			for (String dirTag : indexes.keySet())
			{
				Integer i = indexes.get(dirTag);
				String name = names.get(i);
				String title = titles.get(i);
				String route = routes.get(i);

				ContentValues values = new ContentValues();
				values.put(dirNameKey, name);
				values.put(dirRouteKey, route);
				values.put(dirTagKey, dirTag);
				values.put(dirTitleKey, title);

				if (wipe)
				{
					database.insert(directionsTable, null, values);
				}
				else
				{
					database.replace(directionsTable, null, values);
				}
			}
			database.setTransactionSuccessful();
			database.endTransaction();
		}
		finally
		{
			database.close();
		}
	}

	public synchronized void saveFavorites(HashSet<String> favoriteStops, HashMap<String, StopLocation> sharedStops) {
		SQLiteDatabase database = getWritableDatabase();
		try
		{
			database.beginTransaction();

			database.delete(verboseFavorites, null, null);

			for (String stopTag : favoriteStops)
			{
				StopLocation stopLocation = sharedStops.get(stopTag);
				
				if (stopLocation != null)
				{
					ContentValues values = new ContentValues();
					values.put(latitudeKey, stopLocation.getLatitudeAsDegrees());
					values.put(longitudeKey, stopLocation.getLongitudeAsDegrees());
					database.replace(verboseFavorites, null, values);
				}
			}

			database.setTransactionSuccessful();
			database.endTransaction();

		}
		finally
		{
			database.close();
		}
	}

	public Cursor getCursorForRoutes() {
		throw new RuntimeException("Not Implemented");
	}

	public Cursor getCursorForRoute(String routeName) {
		throw new RuntimeException("Not Implemented");
	}

	public Cursor getCursorForDirections() {
		throw new RuntimeException("Not Implemented");
	}

	public Cursor getCursorForDirection(String dirTag) {
		throw new RuntimeException("Not Implemented");
	}

	public void upgradeIfNecessary() {
		//trigger an upgrade so future calls of getReadableDatabase won't complain that you can't upgrade a read only db
		getWritableDatabase();
		
	}
	
	private class FloatPoint
	{
		public final float lat;
		public final float lon;
		
		public FloatPoint(float lat, float lon)
		{
			this.lat = lat;
			this.lon = lon;
		}
	}

	/**
	 * Read a single stop from the database
	 * @param stopTag
	 * @param transitSystem
	 * @return
	 */
	public StopLocation getStop(String stopTag, TransitSystem transitSystem) {
		SQLiteDatabase database = getReadableDatabase();
		Cursor stopCursor = null;
		try
		{
			//TODO: we should have a factory somewhere to abstract details away regarding subway vs bus

			//get stop with name stopTag, joining with the subway table
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
			String tables = verboseStops +
			" LEFT OUTER JOIN " + subwaySpecificTable + " ON (" + verboseStops + "." + stopTagKey + " = " + 
			subwaySpecificTable + "." + stopTagKey + ")";


			builder.setTables(tables);

			String[] projectionIn = new String[] {latitudeKey, longitudeKey, 
					stopTitleKey, platformOrderKey, branchKey};
			String select = verboseStops + "." + stopTagKey + "=?";
			String[] selectArray = new String[]{stopTag};

			Log.v("BostonBusMap", SQLiteQueryBuilder.buildQueryString(false, tables, projectionIn, verboseStops + "." + stopTagKey + "=\"" + stopTagKey + "\"",
					null, null, null, null));

			stopCursor = builder.query(database, projectionIn, select, selectArray, null, null, null);

			stopCursor.moveToFirst();
			
			if (stopCursor.isAfterLast() == false)
			{
				float lat = stopCursor.getFloat(0);
				float lon = stopCursor.getFloat(1);
				String title = stopCursor.getString(2);
				
				//NOTE: for now, the bus stop icon is the same for all transit sources
				Drawable busStop = transitSystem.getTransitSource(null).getBusStopDrawable();
				
				StopLocation stop;
				if (stopCursor.isNull(3))
				{
					stop = new StopLocation(lat, lon, busStop, stopTag, title);
				}
				else
				{
					int platformOrder = stopCursor.getInt(3);
					String branch = stopCursor.getString(4);
					
					stop = new SubwayStopLocation(lat, lon, busStop, stopTag, title, platformOrder, branch);
				}
				
				return stop;
			}
			else
			{
				return null;
			}
		}
		finally
		{
			if (stopCursor != null)
			{
				stopCursor.close();
			}
			database.close();
		}
	}
}
