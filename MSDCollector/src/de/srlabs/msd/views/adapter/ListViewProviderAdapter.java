package de.srlabs.msd.views.adapter;

import java.util.Vector;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.srlabs.msd.R;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.views.DashboardProviderList;

public class ListViewProviderAdapter extends ArrayAdapter<Risk>
{
	private Context context;
	private Vector<Risk> values;

	public ListViewProviderAdapter(Context context, Vector<Risk> values) 
	{
		super(context, R.layout.custom_row_layout_provider);
		
		this.context = context;
		this.values = values;
	}

	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.custom_row_layout_provider, parent, false);
			
		// Set provider name
		((TextView) rowView.findViewById(R.id.txtProviderName)).setText(values.elementAt(position).getOperatorName());
		
		// Set provider color
		((DashboardProviderList) rowView.findViewById(R.id.dashboardProviderList1)).
			setColor(Color.parseColor(values.elementAt(position).getOperatorColor()));
		
		return rowView;
	}
	
	@Override
	public int getCount() 
	{
		return values.size();
	}
}
