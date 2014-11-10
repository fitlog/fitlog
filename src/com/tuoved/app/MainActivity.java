package com.tuoved.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.tuoved.app.ProviderMetaData.Labels;

@SuppressLint("NewApi")
public class MainActivity extends ListActivity implements LoaderCallbacks<Cursor>, OnClickListener {
	private static final String TAG = "MainActivity";
	
	public final static String EXTRA_MESSAGE = "com.tuoved.app.MESSAGE";
	SimpleCursorAdapter mAdapter;
	protected int mSelectedRowId = 0;
	protected static final int ID_LOADER = 0;
	private final int IDD_EXIT = 0;
	private EditText editText;
	private Button button_add;

	// --------------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.main );

		editText = (EditText) findViewById ( R.id.editMessage );
		button_add = (Button) findViewById ( R.id.buttonSend );
		button_add.setEnabled ( false );
		editText.addTextChangedListener ( editTextWatcher );
		
		final String[] FROM = {Labels.NAME};
		final int[] TO = {android.R.id.text1};
		mAdapter = 	new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				null, FROM, TO , 0);
		getLoaderManager().initLoader(ID_LOADER, null, this);
		setListAdapter(mAdapter);
		getListView().setOnItemLongClickListener(mItemLongClickListener);
		editText.clearFocus();
	}

	// -------------------------------------------------------------------------
	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		showDialog ( IDD_EXIT );
	}
	
	// -------------------------------------------------------------------------	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent ( MainActivity.this,
				ExerciseActivity.class );
		intent.putExtra ( EXTRA_MESSAGE, id );
		if( id > 0 )
			startActivity ( intent );
	}
	// -------------------------------------------------------------------------
	private OnItemLongClickListener mItemLongClickListener = 
			new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			mSelectedRowId = (int)id;
			Toast.makeText(MainActivity.this, "Selected row: " + position, Toast.LENGTH_SHORT)
			.show();
			return true;
		}
	};


	// -------------------------------------------------------------------------
	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
		case IDD_EXIT: {
			AlertDialog.Builder builder = new AlertDialog.Builder ( this );
			builder.setMessage ( "Выйти из приложения?" );
			builder.setPositiveButton ( "Да",
				new DialogInterface.OnClickListener () {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						MainActivity.this.finish ();
					}
				} );
			builder.setNegativeButton ( "Нет",
				new DialogInterface.OnClickListener () {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel ();
					}
				} );
			builder.setCancelable ( false );
			return builder.create ();
		}
		default:
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate ( R.menu.main, menu );
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.deleteitem);
		if(mSelectedRowId == 0 )
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
	// -------------------------------------------------------------------------
	private void addLabel(String label){
		ContentValues cv = new ContentValues();
		cv.put(Labels.NAME, label);
		Uri uri = Labels.CONTENT_URI;
		Uri insUri = getContentResolver().insert(uri, cv);
		Log.d(TAG,"Inserted Uri: " + insUri);
	}
	// -------------------------------------------------------------------------
	private void deleteLabel(){
		Uri uri = Labels.CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(mSelectedRowId)).build();
		getContentResolver().delete(uri, null, null);
		mSelectedRowId = 0;
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
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = Labels.CONTENT_URI;
		String[] projection = { 
				Labels._ID, 
				Labels.NAME
				};
		return new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
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
	@Override
	public void onClick(View v) {
		if( v.getId() == R.id.buttonSend ){
			String label = (String)editText.getText().toString();
			addLabel(label);
			editText.setText("");
			editText.clearFocus();
		}
	}
}
