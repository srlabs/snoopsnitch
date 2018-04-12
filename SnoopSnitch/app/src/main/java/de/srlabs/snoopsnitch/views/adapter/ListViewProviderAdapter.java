package de.srlabs.snoopsnitch.views.adapter;

import java.util.Collections;
import java.util.Vector;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.StartupActivity;
import de.srlabs.snoopsnitch.analysis.Risk;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;
import de.srlabs.snoopsnitch.views.DashboardProviderList;

public class ListViewProviderAdapter extends ArrayAdapter<Risk> {
    private Context context;
    private Vector<Risk> values;
    private Risk risk;
    private final String[] FALLBACK_COLOR_SET = {"#01FF06","#FD0100","#FC4F13","#81007F","#000000","#808080","#30D5C7"};


    public ListViewProviderAdapter(Context context, Vector<Risk> values) {
        super(context, R.layout.custom_row_layout_provider);

        this.context = context;
        this.values = values;

        this.risk = MSDServiceHelperCreator.getInstance().getMsdServiceHelper().getData().getScores();

        sortOwnProvider();

        // Add result data to values list
        values.insertElementAt(risk, 0);

        //correct missing colors
        addMissingOperatorColors(values);
    }

    private void addMissingOperatorColors(Vector<Risk> values) {
        int countFixedOperators = 0;
        for(Risk operator : values){
            String color = operator.getOperatorColor();
            if(color == null || color.equals("")) {
                if(countFixedOperators >= FALLBACK_COLOR_SET.length) {
                    countFixedOperators = 0;
                }
                operator.setOperatorColor(FALLBACK_COLOR_SET[countFixedOperators]);
                countFixedOperators++;
            }
            else{
                try{
                    Color.parseColor(color);
                }catch(IllegalArgumentException e){
                    if(countFixedOperators >= FALLBACK_COLOR_SET.length) {
                        countFixedOperators = 0;
                    }
                    operator.setOperatorColor(FALLBACK_COLOR_SET[countFixedOperators]);
                    countFixedOperators++;
                }
            }
        }
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView;

        // Skip own test results
        if (!StartupActivity.isSNSNCompatible()) {
            position++;
        }

        if (position == 0) {
            rowView = inflater.inflate(R.layout.custom_row_layout_provider, parent, false);

            // Set result
            ((DashboardProviderList) rowView.findViewById(R.id.dashboardProviderList1)).setResult(true);
        } else if (values.elementAt(position).getOperatorName().equals(risk.getOperatorName())) {
            rowView = inflater.inflate(R.layout.custom_row_layout_own_provider, parent, false);

            DashboardProviderList dpl = ((DashboardProviderList) rowView.findViewById(R.id.dashboardProviderList1));

            // Set own provider
            ((DashboardProviderList) rowView.findViewById(R.id.dashboardProviderList1)).setOwnProvider(true);

            // Set provider name
            ((TextView) rowView.findViewById(R.id.txtProviderName)).setText(values.elementAt(position).getOperatorName());

            // Set provider color
            dpl.setColor(Color.parseColor(values.elementAt(position).getOperatorColor()));

        } else {
            rowView = inflater.inflate(R.layout.custom_row_layout_provider, parent, false);

            DashboardProviderList dpl = ((DashboardProviderList) rowView.findViewById(R.id.dashboardProviderList1));

            // Set provider name
            ((TextView) rowView.findViewById(R.id.txtProviderName)).setText(values.elementAt(position).getOperatorName());

            // Set provider color
            dpl.setColor(Color.parseColor(values.elementAt(position).getOperatorColor()));
        }

        rowView.setBackgroundColor(context.getResources().getColor(R.color.common_applicationBackground));


        return rowView;
    }

    @Override
    public int getCount() {
        if (StartupActivity.isSNSNCompatible()) {
            return values.size();
        } else {
            return Math.max(values.size() - 1, 0);
        }
    }

    private void sortOwnProvider() {
        for (Risk r : values) {
            if (values.elementAt(values.indexOf(r)).getOperatorName().equals(risk.getOperatorName())) {
                Collections.swap(values, 0, values.indexOf(r));
            }
        }
    }
}
