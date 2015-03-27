package com.tuoved.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Toast;

import com.tuoved.app.ProviderMetaData.Labels;

public class MainActivity extends FragmentActivity  implements OnClickListener {
	private static final String TAG = "MainActivity";
	private static long mSelectedRowId = 0;
	private static final int ACTION_DELETE = 0;
	
	public final static String EXTRA_MESSAGE = "com.tuoved.app.MESSAGE";
	private EditText editText;
	private Button button_add;

	// -------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.main );

		editText = (EditText) findViewById ( R.id.editMessage );
		button_add = (Button) findViewById ( R.id.buttonSend );
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
		getMenuInflater ().inflate ( R.menu.main, menu );
		return super.onCreateOptionsMenu(menu);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.deleteitem);
		if(mSelectedRowId == 0)
			item.setEnabled(false);
		else
			item.setEnabled(true);
		return super.onPrepareOptionsMenu(menu);
	}
	// -------------------------------------------------------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Log.d("MENU", "Clicked MenuItem is " + item.getTitle());
		switch (item.getItemId ())
		{
		case R.id.deleteitem:
			deleteLabel();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected ( item );
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0, ACTION_DELETE, 0, R.string.delete_record);
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
		default:
			break;
		}
		return super.onContextItemSelected(item);
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
			if (s.toString ().length () == 0)
				button_add.setEnabled ( false );
			else
				button_add.setEnabled ( true );
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
		if( v.getId() == R.id.buttonSend ){
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
			// TODO Auto-generated method stub
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
					intent.putExtra ( EXTRA_MESSAGE, id );
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
