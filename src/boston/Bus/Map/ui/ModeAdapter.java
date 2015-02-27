package boston.Bus.Map.ui;

import java.util.List;

import boston.Bus.Map.util.SelectionResources;

import com.schneeloch.bostonbusmap_library.data.Selection;
import com.schneeloch.torontotransit.R;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;



public class ModeAdapter extends ArrayAdapter<Selection.Mode> {
	private final Activity context;
	
	public ModeAdapter(Activity context, List<Selection.Mode> modes)
	{
		super(context, R.layout.icon, modes);
		
		setDropDownViewResource(R.layout.icon_dropdown);
		
		this.context = context;
	}
	
	//this piece of code was adapted from ContactAdder.java in the Android examples
	
	@Override 
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Inflate a view template
        if (convertView == null) {
            LayoutInflater layoutInflater = context.getLayoutInflater();
            convertView = layoutInflater.inflate(R.layout.icon_dropdown, parent, false);
        }
        
        ImageView icon = (ImageView) convertView.findViewById(R.id.icon_dropdown);
        TextView text = (TextView) convertView.findViewById(R.id.text_dropdown);

        // Populate template
		Selection.Mode data = getItem(position);
        text.setText(SelectionResources.getText(data));

        Drawable drawable = context.getResources().getDrawable(SelectionResources.getDrawable(data));

        icon.setImageDrawable(drawable);
        return convertView;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null)
		{
			LayoutInflater inflater = context.getLayoutInflater();
			convertView = inflater.inflate(R.layout.icon, parent, false);
		}
		
		ImageView icon = (ImageView)convertView.findViewById(R.id.icon);

		Selection.Mode data = getItem(position);
		Drawable drawable = context.getResources().getDrawable(SelectionResources.getDrawable(data));
		icon.setImageDrawable(drawable);
		return convertView;
	}
}
