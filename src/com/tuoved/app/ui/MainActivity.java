package com.tuoved.app.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tuoved.app.R;
import com.tuoved.app.provider.ProviderMetaData.Data;
import com.tuoved.app.provider.ProviderMetaData.Labels;
import com.tuoved.app.ui.LabelsFragment.OnLabelPopupMenuListener;
import com.tuoved.app.utils.Utils;

public class MainActivity extends FragmentActivity  implements OnClickListener, OnLabelPopupMenuListener {
	private static final String TAG = "MainActivity";
	private static final String TAG_CHANGE_DIALOG = "change_dialog";
	private static final String HISTORY_IS_UPDATED = "history_is_updated";
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
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
	
	// -------------------------------------------------------------------------
	private int clear_history(long id) {
		String where = Data.LABEL_ID + "=?";
		return getContentResolver().delete(Data.CONTENT_URI, where, new String[]{String.valueOf(id)});
	}
	
	// -------------------------------------------------------------------------
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
	private TextWatcher editTextWatcher = new TextWatcher ()
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count) {
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
		final int id = v.getId();
		if( id == R.id.buttonAdd ) {
			String label = (String)editText.getText().toString();
			addLabel(this, label);
			editText.setText(null);
			editText.clearFocus();
			Utils.hideKeyboard(this, v);
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
	public void deleteLabel(final long id){
		Uri uri = Labels.CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(id)).build();
		getContentResolver().delete(uri, null, null);
		Log.d(TAG,"Deleted Uri: " + uri);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onChangeLabel(long id) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_CHANGE_DIALOG);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    DialogFragment dlg = ChangeLabelDialog.newInstance(id);
	    dlg.show(ft, TAG_CHANGE_DIALOG);
	}

	// -------------------------------------------------------------------------
	@Override
	public void onClearHistory(long id, String label) {
		if(clear_history(id) > 0) {
			Toast.makeText(this, "История " + label + " очищена", Toast.LENGTH_SHORT).show();
		}
	}

	// -------------------------------------------------------------------------
	@Override
	public void onDeleteLabel(long id) {
		deleteLabel(id);
	}
	
	// -------------------------------------------------------------------------
	public static class ChangeLabelDialog extends DialogFragment implements LoaderCallbacks<Cursor>, OnClickListener {
		private static final String ID_LABEL_KEY = "id";
		private static final int ID_LOADER_CHANGE = 10;
		private String loadedLabel;
		private Uri mUri = null;
		private View mView;
		private EditText etLabel;
		private Button btnChange;
		
		public ChangeLabelDialog() {
			
		}
		
		// -------------------------------------------------------------------------
		public static ChangeLabelDialog newInstance(long id) {
			ChangeLabelDialog dlg = new ChangeLabelDialog();
			Bundle args = new Bundle();
			args.putLong(ID_LABEL_KEY, id);
			dlg.setArguments(args);
			return dlg;
		}
		
		// -------------------------------------------------------------------------
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setStyle(STYLE_NO_TITLE, 0);
			getLoaderManager().restartLoader(ID_LOADER_CHANGE, getArguments(), this);
		}
		
		// -------------------------------------------------------------------------
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mView = inflater.inflate(R.layout.change_label_dialog, container, false);
			etLabel = (EditText)mView.findViewById(R.id.etLabel);
			btnChange = (Button)mView.findViewById(R.id.btnChange);
			btnChange.setOnClickListener(this);
			return mView;
		}
		// -------------------------------------------------------------------------
		@Override
		public void onDestroy() {
			getLoaderManager().destroyLoader(ID_LOADER_CHANGE);
			super.onDestroy();
		}
		
		
		
		// -------------------------------------------------------------------------
		@Override
		public void onDestroyView() {
			mView = null;
			super.onDestroyView();
		}

		// -------------------------------------------------------------------------
		@Override
		public Loader<Cursor> onCreateLoader(int id_loader, Bundle bundle) {
			if(bundle==null)
				return null;
			mUri = Labels.buildLabelsUriWithId(bundle.getLong(ID_LABEL_KEY));
			return new CursorLoader(getActivity(), mUri, new String[]{Labels.NAME}, null, null, null);
		}

		// -------------------------------------------------------------------------
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
			if(loader!=null && c!=null) {
				c.moveToFirst();
				loadedLabel = c.getString(c.getColumnIndex(Labels.NAME));
				etLabel.setText(loadedLabel);
				etLabel.setSelection(etLabel.length());
			}
		}

		// -------------------------------------------------------------------------
		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
		}

		// -------------------------------------------------------------------------
		@Override
		public void onClick(View v) {
			Utils.hideKeyboard(getActivity(), v);
			final int id = v.getId();
			switch(id) {
			case R.id.btnChange:
				updateLabel();
				break;
			}
			dismiss();
		}
		
		// -------------------------------------------------------------------------
		private void updateLabel() {
			if(!checkLabel()) {
				Toast.makeText(getActivity(), R.string.check_entered_data, Toast.LENGTH_SHORT).show();
				return;
			}
			if(!isNeedToUpdate())
				return;
			ContentResolver cr = getActivity().getContentResolver();
			ContentValues cv = new ContentValues();
			cv.put(Labels.NAME, etLabel.getText().toString());
			if(cr.update(mUri, cv, null, null) > 0)
				Toast.makeText(getActivity(), R.string.data_successfully_updated, Toast.LENGTH_SHORT).show();
		}
		
		// -------------------------------------------------------------------------
		private boolean checkLabel() {
			boolean isValid = (etLabel.length()!=0) ? true : false;
			return isValid;
		}
		
		// -------------------------------------------------------------------------
		private boolean isNeedToUpdate() {
			String editedLabel = etLabel.getText().toString();
			return !loadedLabel.equalsIgnoreCase(editedLabel);
		}
		
	}
}
