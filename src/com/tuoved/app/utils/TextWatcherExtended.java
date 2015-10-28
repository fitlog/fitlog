package com.tuoved.app.utils;

import android.text.Editable;
import android.text.NoCopySpan;
import android.view.View;

public interface TextWatcherExtended extends NoCopySpan {

	public void beforeTextChanged(View v, CharSequence s, int start, int count,
			int after);

	public void onTextChanged(View v, CharSequence s, int start, int before,
			int count);

	public void afterTextChanged(View v, Editable s);
	
}
