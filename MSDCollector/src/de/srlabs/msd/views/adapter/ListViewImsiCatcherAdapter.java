package de.srlabs.msd.views.adapter;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Vector;

import android.content.Context;
import android.opengl.Visibility;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.srlabs.msd.DetailChartActivity;
import de.srlabs.msd.R;
import de.srlabs.msd.analysis.ImsiCatcher;

public class ListViewImsiCatcherAdapter extends ArrayAdapter<ImsiCatcher>
{
	// Attributes
	private final Context context;
	private final Vector<ImsiCatcher> values;
	private DetailChartActivity host;
	
	public ListViewImsiCatcherAdapter (Context context, Vector<ImsiCatcher> values) 
	{
		super(context, R.layout.custom_row_layout_sms, values);
	    this.context = context;
	    this.values = values;
	    this.host = (DetailChartActivity) context;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) 
	{
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.custom_row_layout_imsicatcher, parent, false);
		
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
		
		// Check upload state and set button
		Button btnUpload = (Button) rowView.findViewById(R.id.btnUploadImsi);
		
		switch (values.get(position).getUploadState()) 
		{
		case STATE_UPLOADED:
			btnUpload.setBackgroundResource(R.drawable.ic_content_checkmark);
			btnUpload.setText("");
			btnUpload.setEnabled(true);
			btnUpload.setVisibility(View.VISIBLE);
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
					values.get(position).upload();
					Toast.makeText(context, String.valueOf(position),2).show();
					host.refreshView();
				}
			});
			break;
		case STATE_DELETED:
			btnUpload.setBackgroundResource(R.drawable.bt_content_contributedata_disable);
			btnUpload.setText(context.getResources().getString(R.string.common_button_nodata));
			btnUpload.setEnabled(true);
			btnUpload.setVisibility(View.VISIBLE);
			break;
		case STATE_INVALID:
			btnUpload.setBackgroundResource(R.drawable.bt_content_contributedata_disable);
			btnUpload.setText(context.getResources().getString(R.string.common_button_nodata));
			btnUpload.setEnabled(true);
			btnUpload.setVisibility(View.VISIBLE);
			break;
		default:
			btnUpload.setEnabled(false);
			btnUpload.setVisibility(View.GONE);
			break;
		}
	
		return rowView;
	 }
}
