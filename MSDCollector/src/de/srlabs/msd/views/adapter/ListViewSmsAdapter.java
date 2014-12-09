package de.srlabs.msd.views.adapter;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Vector;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import de.srlabs.msd.DetailChartActivity;
import de.srlabs.msd.R;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.analysis.SMS.Type;
import de.srlabs.msd.util.MsdDialog;

public class ListViewSmsAdapter extends ArrayAdapter<SMS> implements Filterable
{
	// Attributes
	private final Context context;
	private final Vector<SMS> allSms;
	private Vector<SMS> values;
	private DetailChartActivity host;
	
	public ListViewSmsAdapter (Context context, Vector<SMS> values) 
	{
		super(context, R.layout.custom_row_layout_sms, values);
	    this.context = context;
	    this.allSms = values;
	    this.values = values;
	    this.host = (DetailChartActivity) context;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) 
	{
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.custom_row_layout_sms, parent, false);
		
		// Set type
		if (values.get(position).getType().name().equals(Type.BINARY_SMS.name()))
		{
			((TextView) rowView.findViewById(R.id.txtSmsRowTypeValue)).setText(context.getResources().getString(R.string.common_binary_sms));			
		}
		else
		{
			((TextView) rowView.findViewById(R.id.txtSmsRowTypeValue)).setText(context.getResources().getString(R.string.common_silent_sms));					
		}
		
		// Set date/time
		TextView txtDateTime = (TextView) rowView.findViewById(R.id.txtSmsRowTimeValue);
		Timestamp stamp = new Timestamp(values.get(position).getTimestamp() + 1000L);
		txtDateTime.setText(DateFormat.getDateTimeInstance().format(stamp.getTime()));
	
		// Set position
		((TextView) rowView.findViewById(R.id.txtSmsRowPositionValue)).setText(String.valueOf(values.get(position).getLatitude()) + " | " + 
				String.valueOf(values.get(position).getLatitude()));
		
		// Set cell id
		((TextView) rowView.findViewById(R.id.txtSmsRowCellIdValue)).setText(String.valueOf(values.get(position).getFullCellID()));
		
		// Set phone number
		((TextView) rowView.findViewById(R.id.txtSmsRowSmsCValue)).setText(values.get(position).getSmsc());
		
		// Set source
		((TextView) rowView.findViewById(R.id.txtSmsRowSourceValue)).setText(values.get(position).getSender());
		
		// Check upload state and set button
		Button btnUpload = (Button) rowView.findViewById(R.id.btnUploadSms);
		
		switch (values.get(position).getUploadState()) 
		{
		case STATE_UPLOADED:
			btnUpload.setBackgroundResource(R.drawable.ic_content_checkmark);
			btnUpload.setText("");
			btnUpload.setEnabled(false);
			btnUpload.setVisibility(View.VISIBLE);
			rowView.setBackgroundColor(context.getResources().getColor(R.color.common_custom_row_background_disabled));
			break;
		case STATE_AVAILABLE:
			btnUpload.setBackgroundResource(R.drawable.bt_content_contributedata_enable);
			btnUpload.setText(context.getResources().getString(R.string.common_button_upload));
			btnUpload.setEnabled(true);
			btnUpload.setVisibility(View.VISIBLE);
			btnUpload.setOnClickListener(new View.OnClickListener() 
			{		
				@Override
				public void onClick(View v) 
				{
					MsdDialog.makeConfirmationDialog(host, host.getResources().getString(R.string.alert_upload_message), 
							new OnClickListener() 
					{			
						@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							values.get(position).upload();
							host.refreshView();
						}
					}, null).show();
				}
			});
			break;
		case STATE_DELETED:
			btnUpload.setBackgroundResource(R.drawable.bt_content_contributedata_disable);
			btnUpload.setText(context.getResources().getString(R.string.common_button_nodata));
			btnUpload.setEnabled(false);
			btnUpload.setVisibility(View.VISIBLE);
			rowView.setBackgroundColor(context.getResources().getColor(R.color.common_custom_row_background_disabled));
			break;
		case STATE_INVALID:
			btnUpload.setBackgroundResource(R.drawable.bt_content_contributedata_disable);
			btnUpload.setText(context.getResources().getString(R.string.common_button_nodata));
			btnUpload.setEnabled(false);
			btnUpload.setVisibility(View.VISIBLE);
			rowView.setBackgroundColor(context.getResources().getColor(R.color.common_custom_row_background_disabled));
			break;
		default:
			btnUpload.setEnabled(false);
			btnUpload.setVisibility(View.GONE);
			break;
		}
		
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
