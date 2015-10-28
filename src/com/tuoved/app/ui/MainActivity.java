package com.tuoved.app.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tuoved.app.R;
import com.tuoved.app.R.id;
import com.tuoved.app.R.layout;
import com.tuoved.app.R.string;
import com.tuoved.app.provider.ProviderMetaData.Data;
import com.tuoved.app.provider.ProviderMetaData.Labels;

public class MainActivity extends FragmentActivity  implements OnClickListener {
	private static final String TAG = "MainActivity";
	private static final String HISTORY_IS_UPDATED = "history_is_updated";
	private static long mSelectedRowId = 0;
	private static final int ACTION_DELETE = 0;
	private static final int ACTION_CLEAR_HISTORY = 1;
	
	public final static String EXTRA_ID_EXERCISE = "com.tuoved.app.id_exercise";
	private EditText editText;
	private Button button_add;

	// -------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.main );
		SharedPreferences sp = getSharedPreferences ("settings", MODE_PRIVATE);
		boolean history_is_updated = sp.getBoolean(HISTORY_IS_UPDATED, false);
		if(!history_is_updated) {
			update_history();
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean(HISTORY_IS_UPDATED, true);
			editor.commit();
		}
		editText = (EditText) findViewById ( R.id.editMessage );
		button_add = (Button) findViewById ( R.id.buttonAdd );
		button_add.setEnabled ( false );
		editText.addTextChangedListener ( editTextWatcher );
		editText.clearFocus();
		
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager
			.beginTransaction()
			.replace(R.id.labels_container, LabelsFragment.newInstance())
			.commit();
	}

	// -------------------------------------------------------------------------
	@Override
	public void onBackPressed() {
		showExitDialog();
	}
	
	// -------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater ().inflate ( R.menu.exercise_list, menu );
		return super.onCreateOptionsMenu(menu);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}
	// -------------------------------------------------------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected ( item );
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0, ACTION_CLEAR_HISTORY, 0, R.string.clear_history);
		menu.add(0, ACTION_DELETE, 0, R.string.delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch(item.getItemId()) {
		case ACTION_DELETE:
			mSelectedRowId = info.id;
			deleteLabel();
			break;
		case ACTION_CLEAR_HISTORY:
			if(clear_history(info.id) > 0)
			{
				TextView tv = (TextView)info.targetView.findViewById(android.R.id.text1);
				String text = tv.getText().toString();
				Toast.makeText(this, "История " + text +" очищена.", Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	private int clear_history(long id) {
		String where = Data.LABEL_ID + "=?";
		return getContentResolver().delete(Data.CONTENT_URI, where, new String[]{String.valueOf(id)});
	}
	
	private void update_history() {
		final String [] PROJECTION = 
				new String[] {
				Data._ID,
				Data.DATE,
				Data.LABEL_ID,
				Data.COUNT_APPROACH,
				Data.COUNT_TRAINING 
		};
		final long MILLIS_OF_TWO_HOUR = 60*60*2*1000;
		final String SORT_ORDER = "data.label_id ASC, data._id ASC";
		ContentResolver cr = getContentResolver();
		
		Cursor c = cr.query(Data.CONTENT_URI, PROJECTION, null, null, SORT_ORDER);
		if(c!=null && c.moveToFirst())
		{
			int id = c.getInt(c.getColumnIndexOrThrow(Data._ID));
			int prev_label_id = c.getInt(c.getColumnIndexOrThrow(Data.LABEL_ID));
			long prev_date = c.getLong(c.getColumnIndexOrThrow(Data.DATE));
			int count_approach = 1;
			int count_training = 1;
			ContentValues values = new ContentValues();
			values.put(Data.COUNT_APPROACH, count_approach);
			values.put(Data.COUNT_TRAINING, count_training);
			Uri uri = Data.buildDataUriWithId(id);
			cr.update(uri, values, null, null);

			while(c.moveToNext()) {
				id = c.getInt(c.getColumnIndexOrThrow(Data._ID));
				int cur_label_id = c.getInt(c.getColumnIndexOrThrow(Data.LABEL_ID));
				long cur_date = c.getLong(c.getColumnIndexOrThrow(Data.DATE));
				if(prev_label_id == cur_label_id) {
					count_approach++;
					if((cur_date-prev_date) > MILLIS_OF_TWO_HOUR)
					{
						count_training++;
						count_approach = 1;
					}
				}
				else {
					count_approach = 1;
					count_training = 1;
					prev_label_id = cur_label_id;
				}
				prev_date = cur_date;
				values.clear();
				values.put(Data.COUNT_APPROACH, count_approach);
				values.put(Data.COUNT_TRAINING, count_training);
				uri = Data.buildDataUriWithId(id);
				cr.update(uri, values, null, null);
			}
			c.close();
		}
	}

	// -------------------------------------------------------------------------
	private void showExitDialog() {
		ExitDialog.getInstance().show(getSupportFragmentManager(), null);
	}

	// -------------------------------------------------------------------------
	private TextWatcher editTextWatcher = new TextWatcher ()
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count)
		{
			button_add.setEnabled ( (s.toString ().length () == 0) ? false :true );
		}
		@Override
		public void afterTextChanged(Editable s) { }
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
			int after) { }
	};

	// -------------------------------------------------------------------------
	@Override
	public void onClick(View v) {
		if( v.getId() == R.id.buttonAdd ){
			String label = (String)editText.getText().toString();
			addLabel(this, label);
			editText.setText(null);
			editText.clearFocus();
		}
	}
	
	// -------------------------------------------------------------------------
	private void addLabel(Context context, String label){
		ContentValues cv = new ContentValues();
		cv.put(Labels.NAME, label);
		Uri uri = Labels.CONTENT_URI;
		Uri insUri = getContentResolver().insert(uri, cv);
		Log.d(TAG,"Inserted Uri: " + insUri);
	}
	// -------------------------------------------------------------------------
	public void deleteLabel(){
		Uri uri = Labels.CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(mSelectedRowId)).build();
		getContentResolver().delete(uri, null, null);
		Log.d(TAG,"Deleted Uri: " + uri);
		mSelectedRowId = 0;
	}
	
	// -------------------------------------------------------------------------
	// LabelsFragment
	// -------------------------------------------------------------------------
	public static class LabelsFragment extends ListFragment implements LoaderCallbacks<Cursor> {
		android.support.v4.widget.SimpleCursorAdapter mAdapter;
		protected static final int ID_LOADER = 0;
		private View rootView;
		
		public static LabelsFragment newInstance(){
			return new LabelsFragment();
		}

		public LabelsFragment(){
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
			mAdapter = 	new android.support.v4.widget.SimpleCursorAdapter(getActivity(),
					android.R.layout.simple_list_item_1,
					null, FROM, TO , 0);
			
			setListAdapter(mAdapter);
			ListView lw = getListView();
			registerForContextMenu(lw);
			lw.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					Intent intent = new Intent ( getActivity(), ExerciseActivity.class );
					intent.putExtra ( EXTRA_ID_EXERCISE, id );
					if( id > 0 )
						getActivity().startActivity ( intent );
				}
			});
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
	
	public static class ExitDialog extends DialogFragment{
		
		public static ExitDialog getInstance() {
			return new ExitDialog();
		}
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder (getActivity());
			builder.setMessage (R.string.exit_question);
			builder.setPositiveButton ( R.string.yes,
				new DialogInterface.OnClickListener () {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getActivity().finish ();
					}
				} );
			builder.setNegativeButton ( R.string.no,
				new DialogInterface.OnClickListener () {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel ();
					}
				} );
			builder.setCancelable ( false );
			return builder.create ();
		}
	}
}
