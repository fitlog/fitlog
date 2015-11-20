package com.tuoved.app.ui;

import java.util.Date;

import android.R.mipmap;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.PopupMenu;
import android.text.Layout;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tuoved.app.R;
import com.tuoved.app.provider.ProviderMetaData.Data;
import com.tuoved.app.provider.ProviderMetaData.Labels;

// -------------------------------------------------------------------------
public class LabelsFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public final static String EXTRA_ID_EXERCISE = "com.tuoved.app.id_exercise";
	LabelsCursorAdapter mAdapter;
	private static final int ID_LOADER = 0;
	private static String NUM_TRAINING = "num_training";
	private static String LAST_DATE = "last_date";
	private View rootView;
	private OnLabelPopupMenuListener mListener;
	private PopupMenu mPopupMenu;
	
	
	// --------------------------------------------------------------------------------------------
	public interface OnLabelPopupMenuListener {
		public void onChangeLabel(long id);
		public void onClearHistory(long id, String label);
		public void onDeleteLabel(long id);
	}
	
	public static LabelsFragment newInstance() {
		return new LabelsFragment();
	}

	public LabelsFragment() {
	};
	
	// -------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getLoaderManager().initLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.labels_fragment, container, false);
		return rootView;
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		getLoaderManager().destroyLoader(ID_LOADER);
		super.onDestroy();
	}
	// -------------------------------------------------------------------------
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		rootView = null;
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAdapter = 	new LabelsCursorAdapter(getActivity(), R.layout.labels_row, null);
		setListAdapter(mAdapter);
		ListView listView = getListView();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent ( getActivity(), ExerciseActivity.class );
				intent.putExtra (EXTRA_ID_EXERCISE, id);
				if(id > 0)
					getActivity().startActivity ( intent );
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				showPopupMenu(view, id);
				return true;
			}
		});
	}
	
	// --------------------------------------------------------------------------------------------
	private void showPopupMenu(final View v, final long id) {
		Context context = getActivity();
		context.setTheme(R.style.Theme_AppCompat);
		mPopupMenu = new PopupMenu(context, v);
		mPopupMenu.inflate(R.menu.label_popup_menu);
		mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if(getUserVisibleHint() == false)
					return false;
				switch(item.getItemId()) {
				case R.id.item_change: {
					mListener.onChangeLabel(id);
					return true;
				}
				case R.id.item_clear_history: {
					TextView tv = (TextView)v.findViewById(R.id.tvLabel);
					String label = tv.getText().toString();
					mListener.onClearHistory(id, label);
					getLoaderManager().restartLoader(ID_LOADER, null, LabelsFragment.this);
					return true;
				}
				case R.id.item_delete: {
					mListener.onDeleteLabel(id);
					return true;
				}
				}
				return false;
			}
		});
		mPopupMenu.show();
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onStop() {
		super.onStop();
		if(mPopupMenu != null)
			mPopupMenu.dismiss();
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try { 
			mListener = (OnLabelPopupMenuListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException( activity.toString() 
					+ " must implemented OnLabelPopupMenuListener");
		}
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	// -------------------------------------------------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int loader, Bundle arg1) {
		Uri uri = Labels.CONTENT_URI;
		
		String[] projection = { 
				Labels._ID,
				Labels.NAME,
				SubQuery.DATA_TRAINING_COUNT,
				SubQuery.DATA_LAST_DATE
				};
		return new CursorLoader( getActivity().getApplicationContext(),
				uri, projection, null, null, null);
	}

	// -------------------------------------------------------------------------
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	// -------------------------------------------------------------------------
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	
	// -------------------------------------------------------------------------
	private class LabelsCursorAdapter extends CursorAdapter{
		private final CharSequence DATE_FORMAT = "Дата: dd.MM.yy";
		private final LayoutInflater mInflater;
		private final int mLayout;
		
		// -------------------------------------------------------------------------
		public LabelsCursorAdapter(Context context, int layout, Cursor c) {
			super(context, c, false);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
		}
		
		// -------------------------------------------------------------------------
		@Override
		public View newView(Context context, Cursor c, ViewGroup parent) {
			View view = mInflater.inflate(mLayout, parent, false);
			ViewHolder holder = new ViewHolder();
			holder.tvLabelName = (TextView)view.findViewById(R.id.tvLabel);
			holder.tvNumTraining = (TextView)view.findViewById(R.id.tvNumTraining);
			holder.tvLastDate = (TextView)view.findViewById(R.id.tvLastDate);
			view.setTag(holder);
			return view;
		}
		
		// -------------------------------------------------------------------------
		@Override
		public void bindView(View v, Context context, Cursor c) {
			ViewHolder holder = (ViewHolder)v.getTag();
			if(holder==null || c==null)
				return;
			if(holder.tvLabelName!=null) {
				String label = c.getString(c.getColumnIndexOrThrow(Labels.NAME));
				holder.tvLabelName.setText(label);
			}
			if(holder.tvLastDate!=null) {
				long last_date = c.getLong(c.getColumnIndexOrThrow(LAST_DATE));
				if(last_date == 0) {
					holder.tvLastDate.setVisibility(View.INVISIBLE);
				}
				else {
					CharSequence dt = DateFormat.format(DATE_FORMAT, last_date);
					holder.tvLastDate.setText(dt);
					holder.tvLastDate.setVisibility(View.VISIBLE);
				}
			}
			if(holder.tvNumTraining!=null) {
				final int num = c.getInt(c.getColumnIndexOrThrow(NUM_TRAINING));
				String num_training = String.valueOf(num);
				holder.tvNumTraining.setText(num_training);
			}
		}
		
		// -------------------------------------------------------------------------
		private class ViewHolder {
			TextView tvLabelName;
			TextView tvNumTraining;
			TextView tvLastDate;
		}
	}
	
	// -------------------------------------------------------------------------
	private interface SubQuery {
		String DATA_TRAINING_COUNT = "(SELECT MAX(" + Data.COUNT_TRAINING + ") "
				+ "FROM " + Data.TABLE_NAME
				+ " WHERE " + Qualified.DATA_LABEL_ID + "=" + Qualified.LABELS_ID 
				+ ") AS " + NUM_TRAINING;
		String DATA_LAST_DATE = "(SELECT MAX(" + Data.DATE + ") "
				+ "FROM " + Data.TABLE_NAME
				+ " WHERE " + Qualified.DATA_LABEL_ID + "=" + Qualified.LABELS_ID 
				+ ") AS " + LAST_DATE;
	}
	
	// -------------------------------------------------------------------------
	private interface Qualified {
		String DATA_LABEL_ID = Data.TABLE_NAME + "." + Data.LABEL_ID;
		String LABELS_ID = Labels.TABLE_NAME + "." + Labels._ID;
	}
	
	
	
}
