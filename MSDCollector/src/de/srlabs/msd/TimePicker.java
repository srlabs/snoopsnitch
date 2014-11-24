package de.srlabs.msd;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class TimePicker extends DialogPreference
{
	// Attributes
    private NumberPicker picker = null;

	public TimePicker(Context ctxt) 
	{
        this(ctxt, null);
    }

    public TimePicker(Context ctxt, AttributeSet attrs) 
    {
        this(ctxt, attrs, 0);
    }

    public TimePicker(Context ctxt, AttributeSet attrs, int defStyle) 
    {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText("Ok");
        setNegativeButtonText("Abbrechen");
    }

    @Override
    protected View onCreateDialogView() 
    {
        picker = new NumberPicker(getContext());
        
        picker.setMinValue(0);
        picker.setMaxValue(24);
        
        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) 
    {
        super.onBindDialogView(v);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) 
    {
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) 
    {
        return (a.getString(index));
    }
    
    
}
