package com.tuoved.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tuoved.app.R;
import com.tuoved.app.provider.ProviderMetaData.Labels;

// -------------------------------------------------------------------------
public class LabelsFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	public final static String EXTRA_ID_EXERCISE = "com.tuoved.app.id_exercise";
	SimpleCursorAdapter mAdapter;
	private static final int ID_LOADER = 0;
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
		final String[] FROM = {Labels.NAME};
		final int[] TO = {android.R.id.text1};
		mAdapter = 	new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_1,
				null, FROM, TO , 0);
		
		setListAdapter(mAdapter);
		ListView listView = getListView();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent ( getActivity(), ExerciseActivity.class );
				intent.putExtra ( EXTRA_ID_EXERCISE, id );
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
					TextView tv = (TextView)v.findViewById(android.R.id.text1);
					String label = tv.getText().toString();
					mListener.onClearHistory(id, label);
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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try { 
			mListener = (OnLabelPopupMenuListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException( activity.toString() 
					+ " must implemented OnLabelPopupMenuListener");
		}
	}
	
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
				Labels.NAME
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
	
}
