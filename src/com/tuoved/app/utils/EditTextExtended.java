package com.tuoved.app.utils;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.EditText;

public class EditTextExtended extends EditText {
	
	private TextWatcherExtended mListener = null;

	public EditTextExtended(Context context) {
		super(context);
	}

	public EditTextExtended(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public EditTextExtended(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	public void addTextChangedListener(TextWatcherExtended watcher) {
		if( mListener == null ) {
			mListener = watcher;
		}
	}
	
	public void removeTextChangedListener(TextWatcherExtended watcher) {
		if( mListener != null ) {
			mListener = null;
		}
	}
	
	void  sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
		if (mListener != null) {
			mListener.beforeTextChanged(this, text, start, before, after);
		}
	}

	void  sendOnTextChanged(CharSequence text, int start, int before,int after) {
		if (mListener != null) {
			mListener.onTextChanged(this, text, start, before, after);
		}
	}

	void  sendAfterTextChanged(Editable text) {
		if (mListener != null) {
			mListener.afterTextChanged(this, text);
		}
	}
}
