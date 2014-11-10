package com.tuoved.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
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
import android.widget.TextView;
import android.widget.Toast;

import com.tuoved.app.ProviderMetaData.Data;
import com.tuoved.app.ProviderMetaData.Labels;

@SuppressLint("NewApi")
public class ExerciseActivity extends ListActivity implements OnClickListener, LoaderCallbacks<Cursor>
{
//	private final int ID_DELETE = 1;
	protected static SimpleCursorAdapter mAdapter;
	Ringtone ringtone;
	RingtoneManager mRingtonManager = new RingtoneManager(this);
	protected static final String TAG = "ExerciseActivity";
	protected static final String SETTINGS_FILE = "settings";
	protected static final String IS_STARTED = "is_started";
	protected static final int ID_LOADER = 0;
	protected static final int ID_LOADER_1 = 1;
	protected static final long MILLIS_SHORT = 20;
	protected static long mLabelRowId = 0;
	protected static String mTitle;
	protected static Button btn_start;
	protected static TextView text_exercise;
	protected static TextView text_timer;
	protected static boolean is_started = false;
	protected static CountDownTimer timer;
	protected static LoaderManager mLoaderManager;
	protected static Vibrator mVibrator;
	protected EditText edit_relax_time, edit_repeats, edit_weight;
	protected int relax_time, repeats;
	protected float weight;
	protected SharedPreferences settings;
	protected int mSelectedRowId = 0;
	

	// --------------------------------------------------------------------------------------------
	// @SuppressLint("ResourceAsColor")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate ( savedInstanceState );
		Log.d ( TAG, "onCreate: " + this.hashCode() );
		setContentView ( R.layout.activity_exercise );
		setupActionBar();
		getViewFromId();
		loadSettings();
		Intent intent = getIntent ();
		mLabelRowId = intent.getLongExtra( MainActivity.EXTRA_MESSAGE, 0 );
		mRingtonManager.setType(RingtoneManager.TYPE_NOTIFICATION);
		mRingtonManager.getCursor();
		ringtone = mRingtonManager.getRingtone(0);
		
		final String[] FROM = {
				Data.DATE,
				Data.WEIGHT,
				Data.REPEATS,
				Data.RELAX_TIME
		};
		int[] to = { R.id.date, R.id.weight, R.id.repeats, R.id.relax };
		mAdapter = new SimpleCursorAdapter(this,
				R.layout.row_item,
				null, FROM , to, 0);
		setListAdapter(mAdapter);
		mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		mLoaderManager = getLoaderManager();
		mLoaderManager.initLoader(ID_LOADER, null, this);
		mLoaderManager.initLoader(ID_LOADER_1, null, this);
		getListView().setOnItemLongClickListener(mItemLongClickListener);
		btn_start.setOnClickListener ( this );
		edit_relax_time.addTextChangedListener ( edit_time_watcher );
		edit_repeats.addTextChangedListener ( editRepeatNumWatcher );
		edit_weight.addTextChangedListener ( edit_weight_watcher );
		
		if(savedInstanceState != null) {
			is_started = savedInstanceState.getBoolean(IS_STARTED);
			CharSequence text = is_started ? "Стоп" : "Старт";
			btn_start.setText(text);
		}
	}
	
	@Override
		protected void onResume() {
		Log.d(TAG, "onResume: " + this.hashCode());
		super.onResume();
		}
	@Override
		protected void onStart() {
		Log.d(TAG, "onStart: " + this.hashCode());
		super.onStart();
		}
	@Override
		protected void onPause() {
		Log.d(TAG, "onPause: " + this.hashCode());
		super.onPause();
		}
	@Override
		protected void onStop() {
		Log.d(TAG, "onStop: " + this.hashCode());
		super.onStop();
		}
	@Override
		protected void onRestart() {
		Log.d(TAG, "onRestart: " + this.hashCode());
		super.onRestart();
		}
	@Override
		protected void onDestroy() {
		Log.d(TAG, "onDestroy: " + this.hashCode());
		super.onDestroy();
		}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_STARTED, is_started );
		super.onSaveInstanceState(outState);
	}
	
	//--------------------------------------------------------------------------
	private OnItemLongClickListener mItemLongClickListener = 
			new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			mSelectedRowId = (int)id;
			mVibrator.vibrate(MILLIS_SHORT);
			Toast.makeText(ExerciseActivity.this, "Selected row: " + position,
					Toast.LENGTH_SHORT).show();
			return false;
		}
	};
	
	//--------------------------------------------------------------------------
	@Override
		protected void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
		}
	//--------------------------------------------------------------------------
	private void getViewFromId(){
		// Initialization layout variables
		btn_start = (Button) findViewById ( R.id.button_Start );
		edit_relax_time = (EditText) findViewById ( R.id.editTextTime );
		edit_repeats = (EditText) findViewById ( R.id.editText_RepeatNum );
		edit_weight = (EditText) findViewById ( R.id.editText_Weight );
		text_timer = (TextView) findViewById ( R.id.timerView );
	}
	
	//--------------------------------------------------------------------------
	private void loadSettings() {
		
		settings = getSharedPreferences ( SETTINGS_FILE, MODE_PRIVATE );
		if (settings.contains ( "RelaxTime" )){
			relax_time = settings.getInt ( "RelaxTime", 30 );
			edit_relax_time.setText ( Integer.toString ( relax_time ) );
		}
		if (settings.contains ( "RepeatNum" )) {
			repeats = settings.getInt ( "RepeatNum", 10 );
			edit_repeats.setText ( Integer.toString ( repeats ) );
		}
		if (settings.contains ( "Weight" ))	{
			weight = settings.getFloat ( "Weight", 15 );
			edit_weight.setText ( Float.toString ( weight ) );
		}
	}

	// -------------------------------------------------------------------------
	private TextWatcher edit_weight_watcher = new TextWatcher ()
	{
		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count)
		{
			try	{
				weight = Float.parseFloat ( s.toString () );
				SharedPreferences settings = getSharedPreferences (
					SETTINGS_FILE, MODE_PRIVATE );
				SharedPreferences.Editor setEditor = settings.edit ();
				setEditor.putFloat ( "Weight", weight );
				setEditor.apply ();
			}
			catch (NumberFormatException e)	{
				weight = 15;
			}
		}
		@Override
		public void afterTextChanged(Editable s) {}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}
	};

	// -------------------------------------------------------------------------
	private TextWatcher edit_time_watcher = new TextWatcher ()
	{

		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count) {
			if (s.toString ().isEmpty ()
				|| Integer.parseInt ( s.toString () ) == 0)
				btn_start.setEnabled ( false );
			else
				btn_start.setEnabled ( true );
			try	{
				relax_time = Integer.parseInt ( s.toString () );
				SharedPreferences settings = getSharedPreferences (
					SETTINGS_FILE, MODE_PRIVATE );
				SharedPreferences.Editor setEditor = settings.edit ();
				setEditor.putInt ( "RelaxTime", relax_time );
				setEditor.apply ();
			}
			catch (NumberFormatException e)	{
				relax_time = 60;
			}
		}

		@Override
		public void afterTextChanged(Editable s) {}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}
	};

	// -------------------------------------------------------------------------
	private TextWatcher editRepeatNumWatcher = new TextWatcher ()
	{

		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count) {
			try	{
				repeats = Integer.parseInt ( s.toString () );
				SharedPreferences settings = getSharedPreferences (
					SETTINGS_FILE, MODE_PRIVATE );
				SharedPreferences.Editor setEditor = settings.edit ();
				setEditor.putInt ( "RepeatNum", repeats );
				setEditor.apply ();
			}
			catch (NumberFormatException e)	{
				repeats = 3;
			}
		}

		@Override
		public void afterTextChanged(Editable s) {}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}
	};

	// -------------------------------------------------------------------------
	private void startTimer() {
		
		final int procOfTime = (int)(0.1 * (double)relax_time);
		final int redColor = getResources().getColor(R.color.red );
		final int greenColor = getResources().getColor(R.color.green);
		
		timer = new CountDownTimer ( 1000 * ( relax_time ), 100 ) {
			StringBuilder builder = new StringBuilder();

			@Override			
			public void onTick(long millisUntilFinished) {
				builder.delete(0, builder.length());
				long sec = millisUntilFinished / 1000;
				if (sec <= procOfTime)
					text_timer.setTextColor (redColor);
				else
					text_timer.setTextColor (greenColor);
				long minutes = sec/60;
				long seconds = sec%60;
				if(minutes < 10)
					builder.append(0);
				builder.append(minutes).append(" : ");
				if(seconds < 10)
					builder.append(0);
				builder.append(seconds);
				text_timer.setText (builder.toString());
				Log.d(TAG, "Timer.onTick:" + sec);
			}
			@Override
			public void onFinish() {
				onClick ( btn_start );
				text_timer.setText ("");
				addExercise(ExerciseActivity.this);
				if(mVibrator.hasVibrator())	{
					long[] pattern = {0, 400, 400, 400, 400, 800};
					mVibrator.vibrate(pattern,-1);
				}
				ringtone.play();
				Log.d(TAG, "Timer.onFinish");
			}
		}.start();
	};

	// -------------------------------------------------------------------------
	@Override
	public void onClick(View v)
	{
		switch (v.getId ())
		{
		case R.id.button_Start:
			if( !is_started ) {
				is_started = true;
				mVibrator.vibrate(MILLIS_SHORT);
				btn_start.setText ("Стоп");
				text_timer.setText ("");
				edit_relax_time.clearFocus();
				edit_repeats.clearFocus();
				edit_weight.clearFocus();
				startTimer();
			}
			else {
				is_started = false;
				btn_start.setText ("Старт");
				text_timer.setText ("");
				if(timer != null)
					timer.cancel ();
			}
			break;
		default:
			return;
		}
	}

	// -------------------------------------------------------------------------
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar()
	{
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)	{
			getActionBar ().setDisplayHomeAsUpEnabled ( true );
		}
	}

	// -------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate ( R.menu.show_message, menu );
		return true;
	}

	// -------------------------------------------------------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId ())
		{
		case R.id.delete_record:
			removeExercise( ExerciseActivity.this );
			break;
			
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask ( this );
			return true;
		}
		return super.onOptionsItemSelected ( item );
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.delete_record);
		if(mSelectedRowId == 0)
			item.setEnabled( false );
		else
			item.setEnabled( true );
		return super.onPrepareOptionsMenu(menu);
	}
	
	// -------------------------------------------------------------------------
	public void addExercise( Context context )
	{
		ContentValues cv = new ContentValues ();
		cv.put ( Data.WEIGHT, (Float)weight );
		cv.put ( Data.REPEATS, (Integer)repeats );
		cv.put ( Data.RELAX_TIME, (Integer)relax_time );
		cv.put( Data.LABEL_ID, mLabelRowId );
		ContentResolver cr = context.getContentResolver ();
		Uri uri = Data.CONTENT_URI;
		Log.d ( TAG, "Exercise insert uri: " + uri );
		Uri insertedUri = cr.insert ( uri, cv );
		Log.d ( TAG, "Inserted URI: " + insertedUri );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	public void removeExercise( Context context )
	{
		if( mSelectedRowId == 0)
			return;
		ContentResolver cr = context.getContentResolver ();
		Uri uri = Data.CONTENT_URI;
		@SuppressWarnings("static-access")
		Uri delUri = uri.withAppendedPath ( uri, Integer.toString ( mSelectedRowId ) );
		Log.d ( TAG, "Del URI: " + delUri );
		int number = cr.delete ( delUri, null, null );
		Log.d ( TAG, "Deleted number: " + number );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
		mSelectedRowId = 0;
	}
	
	// -------------------------------------------------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		Uri uri = null;
		String[] projection = null;
		if( id == ID_LOADER){
			uri = Labels.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(mLabelRowId))
					.appendPath(Data.TABLE_NAME)
					.build();
			projection = new String[] {
					Labels.TABLE_NAME + "." + Labels._ID,
					Labels.NAME,
					Data.TABLE_NAME + "." + Data._ID,
					Data.DATE,
					Data.WEIGHT,
					Data.REPEATS,
					Data.RELAX_TIME,
					Data.LABEL_ID
			};
		}
		else {
			String pathSegment = String.valueOf(mLabelRowId);
			uri = Uri.withAppendedPath(Labels.CONTENT_URI, pathSegment);
		}
		return new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		
		final int id = loader.getId();
		if( id == ID_LOADER )
			mAdapter.swapCursor(data);
		else {
			data.moveToFirst();
			mTitle = data.getString(data.getColumnIndex(Labels.NAME));
			setTitle ( mTitle );
		}
	}

	// -------------------------------------------------------------------------
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		
		final int id = loader.getId();
		if( id == ID_LOADER )
			mAdapter.swapCursor(null);
	}
}

class MyAdapter extends SimpleCursorAdapter{

	public MyAdapter(Context context, int layout, Cursor c, String[] from,
			int[] to, int flags) {
		super(context, layout, c, from, to, flags);
	}
	
	
	
}
