package de.srlabs.msd.views.adapter;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.srlabs.msd.R;
import de.srlabs.msd.analysis.ImsiCatcher;

public class ListViewImsiCatcherAdapter extends ArrayAdapter<ImsiCatcher>
{
	// Attributes
	private final Context context;
	private final Vector<ImsiCatcher> values;
	
	public ListViewImsiCatcherAdapter (Context context, Vector<ImsiCatcher> values) 
	{
		super(context, R.layout.custom_row_layout_sms, values);
	    this.context = context;
	    this.values = values;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		final int pos = position;
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.custom_row_layout_imsicatcher, parent, false);
		
		// Set button
//		Button btnContribute = (Button) rowView.findViewById(R.id.btnDetailListContribute);
//		btnContribute.setOnClickListener(new View.OnClickListener() 
//		{		
//			@Override
//			public void onClick(View v) 
//			{
//				System.out.println(values.get(pos).toString());
//			}
//		});
		
		// Set score
		((TextView) rowView.findViewById(R.id.txtImsiRowScoreValue)).setText(String.valueOf(values.get(position).getScore()));
		
		// Set time
		Timestamp stamp = new Timestamp(values.get(position).getStartTime());
		((TextView) rowView.findViewById(R.id.txtImsiRowTimeValue)).setText(DateFormat.getDateTimeInstance().format(stamp.getTime()));
		
		// Set position
		((TextView) rowView.findViewById(R.id.txtImsiRowPositionValue)).setText(String.valueOf(values.get(position).getLatitude()) + " | " + 
				String.valueOf(values.get(position).getLatitude()));
		
		// Set cell id
		((TextView) rowView.findViewById(R.id.txtImsiRowCellIdValue)).setText(String.valueOf(values.get(position).getFullCellID()));
	
		return rowView;
	 }
}
