package boston.Bus.Map.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.android.maps.MapView;
import com.google.common.collect.ImmutableMap;

import android.util.Log;
import android.widget.Toast;
import boston.Bus.Map.data.Direction;
import boston.Bus.Map.data.RouteTitles;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.TransitSourceTitles;
import boston.Bus.Map.data.UpdateArguments;
import boston.Bus.Map.main.Main;
import boston.Bus.Map.provider.DatabaseAgent;
import boston.Bus.Map.transit.ITransitSystem;
import boston.Bus.Map.transit.TransitSystem;

public class SearchHelper
{
	private final Main context;
	private final RouteTitles dropdownRouteKeysToTitles;
	private final String query;
	private String suggestionsQuery;
	
	private static final int QUERY_NONE = 0;
	private static final int QUERY_ROUTE = 1;
	private static final int QUERY_STOP = 2;
	
	private int queryType = QUERY_NONE;
	
	private final UpdateArguments arguments;
    private final DatabaseAgent databaseAgent;
	
	public SearchHelper(Main context, RouteTitles dropdownRouteKeysToTitles,
			UpdateArguments arguments, String query, DatabaseAgent databaseAgent)
	{
		this.context = context;
		this.dropdownRouteKeysToTitles = dropdownRouteKeysToTitles;
		this.query = query;
		this.arguments = arguments;
        this.databaseAgent = databaseAgent;
	}
	
	/**
	 * Search for query and do whatever actions we do when that happens
	 * @param runnable 
	 * @param query
	 */
	public void runSearch(Runnable onFinish)
	{
		searchRoutes(onFinish);
	}

	/**
	 * Try a search on the list of routes. If it matches, do that. Else, it's a geocode
	 */
	private void searchRoutes(final Runnable onFinish) {
		String lowercaseQuery = query.toLowerCase();
		String printableQuery = query;
		
		//remove these words from the search
		String[] wordsToRemove = new String[] {"route", "subway", "bus", "stop", "direction"};

		queryType = QUERY_NONE;
		String censoredQuery = query;
		for (String wordToRemove : wordsToRemove)
		{
			boolean itEndsWith = lowercaseQuery.endsWith(" " + wordToRemove);
			boolean itStartsWith = lowercaseQuery.startsWith(wordToRemove + " ");
			boolean wholeWord = lowercaseQuery.equals(wordToRemove);
			boolean middleWord = lowercaseQuery.contains(" " + wordToRemove + " ");
			if (itEndsWith || itStartsWith || wholeWord || middleWord)
			{
				String adjustedCensoredQuery;
				if (wholeWord)
				{
					adjustedCensoredQuery = "";
				}
				else if (itEndsWith)
				{
					adjustedCensoredQuery = censoredQuery.substring(0, censoredQuery.length() - (1 + wordToRemove.length()));
				}
				else if (itStartsWith)
				{
					adjustedCensoredQuery = censoredQuery.substring(1 + wordToRemove.length());
				}
				else
				{
					adjustedCensoredQuery = censoredQuery.replace(" " + wordToRemove + " ", "");
				}
				lowercaseQuery = adjustedCensoredQuery.toLowerCase();
				censoredQuery = adjustedCensoredQuery;
				
				if (wordToRemove.equals("route"))
				{
					queryType = QUERY_ROUTE;
				}
				else if (wordToRemove.equals("stop"))
				{
					queryType = QUERY_STOP;
				}
			}
		}
		
		String queryWithoutSpaces = censoredQuery;
		if (censoredQuery.contains(" "))
		{
			queryWithoutSpaces = censoredQuery.replaceAll(" ", "");
			lowercaseQuery = queryWithoutSpaces.toLowerCase();
		}
		
		//indexingQuery is query which may be slightly altered to match one of the route keys
		String indexingQuery = lowercaseQuery;
		if (indexingQuery.length() >= 2)
		{
			//this is kind of a hack. We need subway lines to start with a capital letter to search for them properly
			indexingQuery = indexingQuery.substring(0, 1).toUpperCase() + queryWithoutSpaces.substring(1);
		}
		
		//NOTE: the next section is currently never run since we set queryContainsStop to true if queryContainsRoute was false
		final String finalLowercaseQuery = lowercaseQuery;
		final String finalIndexingQuery = indexingQuery;
		final String finalPrintableQuery = printableQuery;

		returnResults(onFinish, finalIndexingQuery, finalLowercaseQuery, finalPrintableQuery);
	}

	private void returnResults(Runnable onFinish, String indexingQuery, String lowercaseQuery, String printableQuery) {
		final ITransitSystem transitSystem = arguments.getTransitSystem();
		if (queryType == QUERY_NONE || queryType == QUERY_ROUTE)
		{
			int position = getAsRoute(indexingQuery, lowercaseQuery);

			if (position >= 0)
			{
				//done!
				context.setNewRoute(position, false);
				String routeKey = dropdownRouteKeysToTitles.getTagUsingIndex(position);
				String routeTitle = dropdownRouteKeysToTitles.getTitle(routeKey);
				suggestionsQuery = "route " + routeTitle;
			}
			else
			{
				Toast.makeText(context, "Route '" + printableQuery + "' doesn't exist. Did you mistype it?", Toast.LENGTH_LONG).show();
			}
		}
		else if (queryType == QUERY_STOP)
		{
			// ideally we'd use RoutePool instead of DatabaseHelper, since RoutePool will
			// reuse existing stops if they match. But stop is temporary so it doesn't really matter
			String exactQuery;
			if (printableQuery.startsWith("stop "))
			{
				exactQuery = printableQuery.substring(5);
			}
			else
			{
				exactQuery = printableQuery;
			}

			StopLocation stop = databaseAgent.getStopByTagOrTitle(
					lowercaseQuery, exactQuery, transitSystem);
			if (stop != null)
			{	
				context.setNewStop(stop.getFirstRoute(), stop.getStopTag());
				suggestionsQuery = "stop " + stop.getTitle();
			}
			else
			{
				//invalid stop id, or we just didn't parse it correctly
				Toast.makeText(context, "Stop '" + printableQuery + "' doesn't exist. Did you mistype it?", Toast.LENGTH_LONG).show();
			}
		}
		else
		{
			//shouldn't happen
			Log.e("BostonBusMap", "Error: query is neither about stops, routes, or directions");
		}
		
		onFinish.run();
	}

	private int getAsRoute(String indexingQuery, String lowercaseQuery)
	{
		String route = arguments.getTransitSystem().searchForRoute(indexingQuery, lowercaseQuery);
		if (route != null)
		{
			return dropdownRouteKeysToTitles.getIndexForTag(route);
		}
		else
		{
			return -1;
		}
	}

	public String getSuggestionsQuery()
	{
		return suggestionsQuery;
	}

	public static String naiveSearch(String indexingQuery, String lowercaseQuery,
			TransitSourceTitles routeKeysToTitles)
	{
		if (routeKeysToTitles.hasRoute(indexingQuery))
		{
			return indexingQuery;
		}
		else
		{
			//try the titles
			for (String route : routeKeysToTitles.routeTags()) {
				String title = routeKeysToTitles.getTitle(route);
				String titleWithoutSpaces = title.toLowerCase().replaceAll(" ", "");
				if (titleWithoutSpaces.equals(lowercaseQuery)) {
					return route;
				}
			}
			
			//no match
			return null;
		}
	}
}
