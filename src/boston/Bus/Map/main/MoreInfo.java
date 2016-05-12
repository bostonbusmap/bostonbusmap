package boston.Bus.Map.main;

import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.schneeloch.bostonbusmap_library.data.IPrediction;
import com.schneeloch.bostonbusmap_library.data.TimeBounds;
import boston.Bus.Map.ui.TextViewBinder;
import com.schneeloch.bostonbusmap_library.util.MoreInfoConstants;
import com.schneeloch.torontotransit.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class MoreInfo extends ListActivity {
	public static final String predictionsKey = "predictions";
	public static final String stopsKey = "stops";
    public static final String placesKey = "places";
	
	public static final String titleKey = "title";
    public static final String snippetTitleKey = "snippetTitle";
	public static final String routeTitlesKey = "route";
	
	public static final String routeTextKey = "routeText";
	public static final String stopIsBetaKey = "stopIsBeta";
	
	public static final String boundKey = "bounds";
	
	private IPrediction[] predictions;
	private TextView title1;
	private TextView title2;
	private Spinner routeSpinner;
	
	/**
	 * If false, don't try accessing predictions or routeKeysToTitles because they may be being populated
	 */
	private boolean dataIsInitialized;
	private String[] routeTitles;
	private TimeBounds[] bounds;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.moreinfo);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		final Bundle extras = getIntent().getExtras();

        setTitle(Html.fromHtml(extras.getString(snippetTitleKey)));

		
		{
			Parcelable[] parcelables = extras.getParcelableArray(predictionsKey);
			predictions = new IPrediction[parcelables.length];
			for (int i = 0; i < predictions.length; i++)
			{
				predictions[i] = (IPrediction)parcelables[i];
			}
		}
		
		{
			Parcelable[] boundParcelables = extras.getParcelableArray(boundKey);
			bounds = new TimeBounds[boundParcelables.length];
			for (int i = 0; i < bounds.length; i++) {
				bounds[i] = (TimeBounds)boundParcelables[i];
			}
		}
		
		
		title1 = (TextView)findViewById(R.id.moreinfo_title1);
		title2 = (TextView)findViewById(R.id.moreinfo_title2);
		routeSpinner = (Spinner)findViewById(R.id.moreinfo_route_spinner);
		
		routeTitles = extras.getStringArray(routeTitlesKey);
		refreshRouteAdapter();
		
		dataIsInitialized = true;
		
		refreshAdapter(null);
		
		routeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if (routeTitles == null)
				{
					return;
				}
				
				if (position == 0)
				{
					refreshAdapter(null);
					refreshText(extras, null);
				}
				else
				{
					int index = position - 1;
					if (index < 0 || index >= routeTitles.length)
					{
						Log.e("BostonBusMap", "error, went past end of route list");
					}
					else
					{
						refreshAdapter(routeTitles[index]);
						refreshText(extras, routeTitles[index]);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
                //leave the state the way it is
			}
		});

		refreshText(extras, null);
	}

	private void refreshText(Bundle extras, String routeTitle) {
		boolean stopIsBeta = extras.getBoolean(stopIsBetaKey);
		
		String[] stopTitles = extras.getStringArray(titleKey);
		
		StringBuilder titleText1 = new StringBuilder();
		for (int i = 0; i < stopTitles.length; i++)
		{
			titleText1.append(stopTitles[i]);
			if (i != stopTitles.length - 1)
			{
				titleText1.append("<br />");
			}
		}

		StringBuilder titleText2 = new StringBuilder();
		String stopTags = extras.getString(stopsKey);
        titleText2.append("<br />Stop ids: ").append(stopTags);
        titleText2.append("<br />");

        title1.setText(Html.fromHtml(titleText1.toString()));
		title2.setText(Html.fromHtml(titleText2.toString()));
	}

	private void refreshRouteAdapter()
	{
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
		if (routeTitles == null)
		{
			//shouldn't happen, but just in case
			adapter.add("All routes");
		}
		else
		{
			if (routeTitles.length != 1)
			{
				//if there's only one route, don't bother with this
				adapter.add("All routes");
			}
			for (String routeTitle : routeTitles)
			{
				adapter.add(routeTitle);
			}
		}
		
		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		routeSpinner.setAdapter(adapter);
	}

	private void refreshAdapter(String routeTitle)
	{
		if (!dataIsInitialized)
		{
			return;
		}
		
		List<Map<String, Spanned>> data = Lists.newArrayList();
		if (predictions != null)
		{
			for (IPrediction prediction : predictions)
			{
				if (prediction != null && !prediction.isInvalid())
				{
					//if a route is given, filter based on it, else show all routes
					if (routeTitle == null || routeTitle.equals(prediction.getRouteTitle()))
					{
						//data.add(prediction.generateMoreInfoMap());
						ImmutableMap<String, Spanned> map = prediction.makeSnippetMap();
						data.add(map);
					}
				}
			}
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.moreinfo_row,
				new String[]{MoreInfoConstants.textKey},
				new int[] {R.id.moreinfo_text});
		
		adapter.setViewBinder(new TextViewBinder());

		setListAdapter(adapter);
		
	}
}
