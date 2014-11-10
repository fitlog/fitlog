package com.tuoved.app;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public class DBCursorAdapter extends SimpleCursorAdapter {
	private final LayoutInflater inflater;
	private int layout;

	@SuppressWarnings("deprecation")
	public DBCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.inflater = LayoutInflater.from(context);
		this.layout = layout;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return inflater.inflate( layout, parent, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		
	}

}
