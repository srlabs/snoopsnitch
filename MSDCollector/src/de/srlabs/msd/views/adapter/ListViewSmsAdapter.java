package de.srlabs.msd.views.adapter;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

import de.srlabs.msd.R;
import de.srlabs.msd.analysis.SMS;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class ListViewSmsAdapter extends ArrayAdapter<SMS> implements Filterable
{
	// Attributes
	private final Context context;
	private final Vector<SMS> allSms;
	private Vector<SMS> values;
	
	public ListViewSmsAdapter (Context context, Vector<SMS> values) 
	{
		super(context, R.layout.custom_row_layout_sms, values);
	    this.context = context;
	    this.allSms = values;
	    this.values = values;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.custom_row_layout_sms, parent, false);
		
		// Set type
		TextView txtType = (TextView) rowView.findViewById(R.id.txtDetailListSmsType);
		if (values.get(position).getType().equals(SMS.Type.BINARY_SMS))
		{
			txtType.setText("Binary SMS");
		}
		else
		{
			txtType.setText("Silent SMS");
		}
		
		// Set date/time
		TextView txtDateTime = (TextView) rowView.findViewById(R.id.txtDetailListDateTime);
		Timestamp stamp = new Timestamp(values.get(position).getTimestamp());
		;
		new SimpleDateFormat();
		txtDateTime.setText(DateFormat.getDateTimeInstance().format(stamp.getTime()));
	
		return rowView;
	 }
	
	@Override
	public Filter getFilter() 
	{
		return new Filter() 
		{		
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) 
			{
				if (results.count == 0)
				{
					//notifyDataSetInvalidated();
					values = (Vector<SMS>) results.values;
					notifyDataSetChanged();
				}
				else
				{
					values = (Vector<SMS>) results.values;
					notifyDataSetChanged();
				}
			}
		
			@Override
			protected FilterResults performFiltering(CharSequence smsType) 
			{
				FilterResults results = new FilterResults();
				Vector<SMS> smsList = new Vector<SMS>();
				
				if (smsType.equals("ALL"))
				{
					results.values = allSms;
					results.count = allSms.size();
					return results;
				}
				
				for (SMS sms : allSms) 
				{
					if (sms.getType().toString().equals(smsType))
					{
						smsList.add(sms);
					}
				}
				
				results.values = smsList;
				results.count = smsList.size();
				
				return results;
			}
		};
	}
	
	@Override
	public int getCount() 
	{
		return values.size();
	}
}
