package com.tuoved.app.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tuoved.app.R;
import com.tuoved.app.provider.ProviderMetaData.Data;
import com.tuoved.app.provider.ProviderMetaData.Labels;

// -------------------------------------------------------------------------
public class ExerciseListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	private static final String KEY_NUM_TRAINING = "num_training";
	private static final String KEY_POSITION_PAGER = "position_pager";
	private static final String KEY_LABEL_ID = "label_id";
	
	private static final int ACTION_DELETE = 1;
	private static final int ACTION_CHANGE = 0;
	private final int ID_LIST_LOADER = 100;
	private ListView mListView;
	private View mView;
	private TextView tvNoData;
	private ExerciseListAdapter mListAdapter;
	private OnExerciseContextMenuListener mListener;
	
	// --------------------------------------------------------------------------------------------
	public interface OnExerciseContextMenuListener {
		public void onChangeData(long id);
		public void onDeleteData(long id);
	}
	
	// --------------------------------------------------------------------------------------------
	public ExerciseListFragment(){}

	// --------------------------------------------------------------------------------------------
	static ExerciseListFragment newInstance(int pos_pager, int num_training, long label_id ) {
		ExerciseListFragment f = new ExerciseListFragment();
		Bundle args = new Bundle();
		args.putInt(KEY_POSITION_PAGER, pos_pager);
		args.putInt(KEY_NUM_TRAINING, num_training);
		args.putLong(KEY_LABEL_ID, label_id);
		f.setArguments(args);
		return f;
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getLoaderManager().restartLoader(ID_LIST_LOADER, getArguments(), this);
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.exercise_list_fragment, container, false);
		tvNoData = (TextView)mView.findViewById(R.id.tv_no_data);
		return mView;
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListAdapter = new ExerciseListAdapter(getActivity(), R.layout.row_item, null);
		setListAdapter(mListAdapter);
		mListView = getListView();
		registerForContextMenu(mListView);
	}
	
	// --------------------------------------------------------------------------------------------
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnExerciseContextMenuListener)activity;
		} catch(ClassCastException e) {
			throw new ClassCastException(activity.toString() 
					+ " must implement OnExerciseContextMenuListener");
		}
	}
	
	// --------------------------------------------------------------------------------------------
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0, ACTION_CHANGE, 0, R.string.change);
		menu.add(0, ACTION_DELETE, 0, R.string.delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	// --------------------------------------------------------------------------------------------
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if( getUserVisibleHint() == false )
	        return false;
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch(item.getItemId()) {
		case ACTION_CHANGE: {
			mListener.onChangeData(info.id);
			break;
		}
		case ACTION_DELETE:	{
			mListener.onDeleteData(info.id);
			break;
		}
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getLoaderManager().destroyLoader(ID_LIST_LOADER);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mView = null;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int id_loader, Bundle bundle) {
		if(bundle==null || id_loader!=ID_LIST_LOADER)
			return null;
		long label_id = bundle.getLong(KEY_LABEL_ID);
		Uri uri = Labels.CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(label_id))
				.appendPath(Data.TABLE_NAME)
				.build();
		String[] projection = new String[] {
				Labels.TABLE_NAME + "." + Labels._ID,
				Labels.NAME,
				Data.TABLE_NAME + "." + Data._ID,
				Data.DATE,
				Data.WEIGHT,
				Data.REPEATS,
				Data.RELAX_TIME,
				Data.LABEL_ID,
				Data.COUNT_APPROACH,
				Data.COUNT_TRAINING
		};
		int num_training = bundle.getInt(KEY_NUM_TRAINING);
		int position = bundle.getInt(KEY_POSITION_PAGER);
		String selection = Data.COUNT_TRAINING + "=?";
		String[] selectionArgs = new String[]{String.valueOf(num_training - position)};
		return new CursorLoader(getActivity(), uri, projection, selection, selectionArgs, null);
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		mListAdapter.swapCursor(c);
		if(c == null || c.getCount()==0)
			return;
		tvNoData.setVisibility(View.GONE);
		final int pos = c.getCount()-1;
		mListView.post(new Runnable() {
			@Override
			public void run() {
				mListView.setSelection(pos);
				View v = mListView.getChildAt(pos);
				if (v != null) {
					v.requestFocus();
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mListAdapter.swapCursor(null);
	}
	
	// -------------------------------------------------------------------------
	private class ViewHolder{
		TextView tvHeader;
		TextView tvTime;
		TextView tvWeight;
		TextView tvRepeats;
		TextView tvRelax;
	}

	// -------------------------------------------------------------------------
	private class ExerciseListAdapter extends CursorAdapter {
		private final CharSequence DATE_FORMAT = "dd-MM-yy (EEE) kk:mm";

		private LayoutInflater mInflater;
		private int mLayout;

		// --------------------------------------------------------------------------------------------
		public ExerciseListAdapter(Context context, int layout, Cursor c) {
			super(context, c, false);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
		}

		// --------------------------------------------------------------------------------------------
		@Override
		public View newView(Context context, Cursor c, ViewGroup parent) {
			View v = mInflater.inflate(mLayout, parent, false);
			ViewHolder holder = new ViewHolder();
			holder.tvHeader = (TextView)v.findViewById(R.id.header);
			holder.tvTime = (TextView)v.findViewById(R.id.time);
			holder.tvWeight = (TextView)v.findViewById(R.id.weight);
			holder.tvRepeats = (TextView)v.findViewById(R.id.repeats);
			holder.tvRelax = (TextView)v.findViewById(R.id.relax);
			v.setTag(holder);
			return v;
		}

		// --------------------------------------------------------------------------------------------
		@Override
		public void bindView(View v, Context context, Cursor c) {
			ViewHolder holder = (ViewHolder) v.getTag();
			int pos_cur = c.getPosition();
			int training_prev = 0;
			int pos_prev = pos_cur-1;
			if(pos_prev >= 0) {
				c.moveToPosition(pos_prev);
				training_prev = c.getInt(c.getColumnIndexOrThrow(Data.COUNT_TRAINING));
			}
			c.moveToPosition(pos_cur);
			long time_cur_ms = c.getLong(c.getColumnIndexOrThrow(Data.DATE));
			int training_cur = c.getInt(c.getColumnIndexOrThrow(Data.COUNT_TRAINING));

			CharSequence dt = null;
			if(holder.tvHeader != null) {
				if( training_cur != training_prev ) {
					dt = DateFormat.format(DATE_FORMAT, time_cur_ms);
					int count_training = c.getInt(c.getColumnIndexOrThrow(Data.COUNT_TRAINING));
					holder.tvHeader.setText("["+String.valueOf(count_training)+"] "+dt);
					holder.tvHeader.setVisibility(View.VISIBLE);
				}
				else {
					holder.tvHeader.setVisibility(View.GONE);
				}
			}

			int count_approach = c.getInt(c.getColumnIndexOrThrow(Data.COUNT_APPROACH));
			if(holder.tvTime != null) {
				holder.tvTime.setText(String.valueOf(count_approach));
			}

			String weight = c.getString(c.getColumnIndexOrThrow(Data.WEIGHT));
			if(holder.tvWeight != null)
				holder.tvWeight.setText(weight);

			String repeats = c.getString(c.getColumnIndexOrThrow(Data.REPEATS));
			if(holder.tvRepeats != null)
				holder.tvRepeats.setText(repeats);

			long time_relax = c.getLong(c.getColumnIndexOrThrow(Data.RELAX_TIME));
			String relax = time_relax == 0 ? "-" : String.valueOf(time_relax);
			if(holder.tvRelax != null)
				holder.tvRelax.setText(relax);
		}

	}
}
