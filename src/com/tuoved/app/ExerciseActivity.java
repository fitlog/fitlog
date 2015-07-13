package com.tuoved.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tuoved.app.MainActivity.ExitDialog;
import com.tuoved.app.ProviderMetaData.Data;
import com.tuoved.app.ProviderMetaData.Labels;

public class ExerciseActivity extends FragmentActivity implements OnClickListener, LoaderCallbacks<Cursor>, OnFocusChangeListener
{
	protected static final String TAG = "ExerciseActivity";
	
	private static MyAdapter mAdapter;
	private static LoaderManager mLoaderManager;
	private static Ringtone mRingtone;
	private static RingtoneManager mRingtonManager;
	private static Vibrator mVibrator;
	private static PowerManager mPowerManager;
	private static PowerManager.WakeLock mWakeLock;
	
	protected static final String SETTINGS_FILE = "settings";
	protected static final String IS_STARTED = "is_started";
	private static final int ACTION_DELETE = 0;
	protected static final int ID_LOADER = 0;
	protected static final long VIBR_MILLIS_SHORT = 20;
	protected static long mLabelRowId = 0;
	protected static String mTitle;
	protected static Button btn_start;
	protected static TextView text_exercise;
	protected static TextView text_timer;
	protected static ListView mListView;
	protected static boolean is_started = false;
	protected static CountDownTimer timer;
	
	protected EditText edit_relax_time, edit_repeats, edit_weight;
	protected int relax_time, repeats;
	private long past_time;
	protected float weight;
	protected SharedPreferences settings;
	protected long mSelectedRowId = 0;
	

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
		setLabel(getIntent());
		fillData();
		registerListeners();
		mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mRingtonManager = new RingtoneManager(this);
		mRingtonManager.setType(RingtoneManager.TYPE_NOTIFICATION);
		mRingtonManager.getCursor();
		if( mRingtonManager.getCursor().getCount() != 0 )
			mRingtone = mRingtonManager.getRingtone( 0 );
		mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		
		if(savedInstanceState != null) {
			is_started = savedInstanceState.getBoolean(IS_STARTED);
			CharSequence text = is_started ? "Стоп" : "Старт";
			btn_start.setText(text);
		}

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
			removeExercise(ExerciseActivity.this);
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
		protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		}
	@Override
		protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
		}
	@Override
		protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		}
	@Override
		protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
		}
	@Override
		protected void onRestart() {
		Log.d(TAG, "onRestart");
		super.onRestart();
		}
	@SuppressLint("Wakelock")
	@Override
		protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		if(timer!=null)
			timer.cancel();
		if(mWakeLock.isHeld())
			mWakeLock.release();
		super.onDestroy();
		}
	
	@Override
	public void onBackPressed() {
		if( is_started )			
		{
			is_started = false;
			btn_start.setText (R.string.button_add);
			text_timer.setText ("");
			if( timer != null )
				timer.cancel();
		}
		else
			super.onBackPressed();
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_STARTED, is_started );
		super.onSaveInstanceState(outState);
	}
	
	
	//--------------------------------------------------------------------------
	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			
		}
	};
	//--------------------------------------------------------------------------
	private void getViewFromId() {
		// Initialization layout variables
		btn_start = (Button) findViewById ( R.id.button_Start );
		edit_relax_time = (EditText) findViewById ( R.id.editTextTime );
		edit_repeats = (EditText) findViewById ( R.id.editText_RepeatNum );
		edit_weight = (EditText) findViewById ( R.id.editText_Weight );
		text_timer = (TextView) findViewById ( R.id.timerView );
		mListView = (ListView) findViewById(R.id.list_data);
		if(mListView!=null)
			registerForContextMenu(mListView);
		
	}
	//--------------------------------------------------------------------------
	private void setLabel(Intent intent){
		if( intent == null ) {
			this.setTitle ( mTitle );
			return;
		}
		mLabelRowId = intent.getLongExtra( MainActivity.EXTRA_MESSAGE, 0 );
		String pathSegment = String.valueOf(mLabelRowId);
		final Uri uri = Labels.CONTENT_URI.buildUpon().appendPath(pathSegment).build();
		Cursor cursor = getContentResolver().query(uri, null, null, null, null);
		if(cursor != null) {
			cursor.moveToFirst();
			mTitle = cursor.getString(cursor.getColumnIndex(Labels.NAME));
			cursor.close();
			cursor = null;
		}
		else
			mTitle = "Exercises";
		this.setTitle ( mTitle );
	}
	
	//--------------------------------------------------------------------------
	private void loadSettings() {
		settings = getSharedPreferences ( SETTINGS_FILE, MODE_PRIVATE );
		relax_time = settings.getInt ( "RelaxTime", 30 );
		edit_relax_time.setText ( Integer.toString ( relax_time ) );
		repeats = settings.getInt ( "RepeatNum", 10 );
		edit_repeats.setText ( Integer.toString ( repeats ) );
		weight = settings.getFloat ( "Weight", 15 );
		edit_weight.setText ( Float.toString ( weight ) );
	}
	
	//--------------------------------------------------------------------------
	private void fillData() {
		mLoaderManager = getSupportLoaderManager();
		mLoaderManager.initLoader(ID_LOADER, null, this);
		final String[] FROM = {
				Data.DATE,
				Data.WEIGHT,
				Data.REPEATS,
				Data.RELAX_TIME
		};
		int[] to = { R.id.time, R.id.weight, R.id.repeats, R.id.relax };
		mAdapter = new MyAdapter(this,
				R.layout.row_item,
				null, FROM, to, 0);
		mListView.setAdapter(mAdapter);
	}
	
	private void registerListeners() {
//		getListView().setOnItemLongClickListener(mItemLongClickListener);
		btn_start.setOnClickListener ( this );
		edit_weight.addTextChangedListener ( edit_weight_watcher );
		edit_weight.setOnFocusChangeListener(this);
		edit_weight.setSelectAllOnFocus(true);
		edit_repeats.addTextChangedListener ( editRepeatNumWatcher );
		edit_repeats.setOnFocusChangeListener(this);
		edit_repeats.setSelectAllOnFocus(true);
		edit_relax_time.addTextChangedListener ( edit_time_watcher );		
		edit_relax_time.setOnFocusChangeListener(this);
		edit_relax_time.setSelectAllOnFocus(true);
		mListView.setOnItemClickListener(mItemClickListener);
		
	}

	// -------------------------------------------------------------------------
	private TextWatcher edit_weight_watcher = new TextWatcher ()
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count)
		{
			try	{
				weight = Float.parseFloat ( s.toString () );
			}
			catch (NumberFormatException e)	{
				weight = 0;
			} finally {
				savePreferences("Weight", weight);
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
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			try	{
				relax_time = Integer.parseInt ( s.toString () );
			}
			catch (NumberFormatException e)	{
				relax_time = 0;
			} finally {
				savePreferences("RelaxTime", relax_time);
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
			}
			catch (NumberFormatException e)	{
				repeats = 3;
			} finally {
				savePreferences( "RepeatNum", repeats);
			}
		}	

		@Override
		public void afterTextChanged(Editable s) {}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}
	};
	
	// -------------------------------------------------------------------------
	void savePreferences(String key, int value) {
		SharedPreferences settings = getSharedPreferences (
				SETTINGS_FILE, MODE_PRIVATE );
		SharedPreferences.Editor setEditor = settings.edit ();
		setEditor.putInt ( key, value );
		setEditor.commit();
	}
	
	// -------------------------------------------------------------------------
	void savePreferences(String key, float value) {
		SharedPreferences settings = getSharedPreferences (
				SETTINGS_FILE, MODE_PRIVATE );
		SharedPreferences.Editor setEditor = settings.edit ();
		setEditor.putFloat( key, value );
		setEditor.commit();
	}
	
	// -------------------------------------------------------------------------
	public void onFocusChange(View v, boolean hasFocus) {
		// TODO Auto-generated method stub
		switch( v.getId() )
		{
		case R.id.editText_Weight:
			edit_weight.setSelection(0, edit_weight.getText().length() );
			break;
		case R.id.editText_RepeatNum:
			edit_repeats.setSelection(0, edit_repeats.getText().length() );
			break;
		case R.id.editTextTime:
			edit_relax_time.setSelection(0, edit_relax_time.getText().length());
			break;
		}
	}

	// -------------------------------------------------------------------------
	private void startTimer() {
		
		final int procOfTime = (int)(0.1 * (double)relax_time);
		final int redColor = getResources().getColor(R.color.red );
		final int greenColor = getResources().getColor(R.color.green);
		if(relax_time==0) {
			past_time = 0;
			onClick( btn_start );
			return;
		}
		mWakeLock.acquire();
		timer = new CountDownTimer ( 1000 * ( relax_time ), 500 ) {
			StringBuilder builder = new StringBuilder();

			@Override			
			public void onTick(long millisUntilFinished) {
				builder.delete(0, builder.length());
				long sec = millisUntilFinished / 1000;
				past_time = relax_time - sec;
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
//				Log.d(TAG, "Timer.onTick:" + sec);
			}
			@Override
			public void onFinish() {
				onClick ( btn_start );
				long[] pattern = {0, 400, 400, 400, 400, 800};
				mVibrator.vibrate(pattern,-1);
				try {
					if(mRingtone != null)
						mRingtone.play();
				}
				catch( NullPointerException e ){
					Log.e(TAG, "Ringtone is null", e);
				}
				Log.d(TAG, "Timer.onFinish");
				if(mWakeLock.isHeld())
					mWakeLock.release();
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
				mVibrator.vibrate(VIBR_MILLIS_SHORT);
				btn_start.setText ("Продолжить");
				text_timer.setText ("");
				edit_relax_time.clearFocus();
				edit_repeats.clearFocus();
				edit_weight.clearFocus();
				startTimer();
			}
			else {
				is_started = false;
				btn_start.setText (R.string.button_add);
				text_timer.setText ("");
				addExercise(ExerciseActivity.this);
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
	private void setupActionBar() {
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
	public boolean onOptionsItemSelected(MenuItem item)	{
		switch (item.getItemId ()) {
		case R.id.delete_record: {
			removeExercise( ExerciseActivity.this );
			break;
		}
		case android.R.id.home: {
			onBackPressed();
			return true;
		}
		}
		return super.onOptionsItemSelected ( item );
	}
	
	// -------------------------------------------------------------------------
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
		cv.put ( Data.RELAX_TIME, (Long)past_time );
		cv.put( Data.LABEL_ID, mLabelRowId );
		ContentResolver cr = context.getContentResolver ();
		Uri uri = Data.CONTENT_URI;
		Log.d ( TAG, "Exercise insert uri: " + uri );
		Uri insertedUri = cr.insert ( uri, cv );
		Log.d ( TAG, "Inserted URI: " + insertedUri );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	public void removeExercise( Context context ) {
		if( mSelectedRowId == 0)
			return;
		ContentResolver cr = context.getContentResolver ();
		Uri uri = Data.CONTENT_URI;
//		@SuppressWarnings("static-access")
		Uri delUri = uri.buildUpon().appendPath(Long.toString (mSelectedRowId)).build();
		Log.d ( TAG, "Del URI: " + delUri );
		int number = cr.delete ( delUri, null, null );
		Log.d ( TAG, "Deleted number: " + number );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
		mSelectedRowId = 0;
	}
	
	// -------------------------------------------------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int loader,
			Bundle args) {
		Uri uri = null;
		String[] projection = null;
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
		return new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader,
			Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	// -------------------------------------------------------------------------
	private class MyAdapter extends SimpleCursorAdapter {
		private final CharSequence DATE_FORMAT = "dd-MM-yy (EEE)";
		private final CharSequence TIME_FORMAT = "kk:mm";
		private final long MILLIS_OF_DAY = 60*60*24*1000;
		private LayoutInflater mInflater;
		private int mLayout;

		public MyAdapter(Context context, int layout, Cursor c, String[] from,
				int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
		}

		@Override
		public View newView(Context context, Cursor c, ViewGroup parent) {
			return mInflater.inflate(mLayout, parent, false);
		}
		
		@Override
		public void bindView(View v, Context context, Cursor c) {
			if(c == null)
				return;
			int pos = c.getPosition();
			long prev_millis = 0;
			if((pos - 1) >= 0) {
				c.moveToPosition(pos - 1);
				prev_millis = c.getLong(c.getColumnIndexOrThrow(Data.DATE));
				prev_millis -= prev_millis % MILLIS_OF_DAY;
			}
			c.moveToPosition(pos);
			long cur_millis = c.getLong(c.getColumnIndexOrThrow(Data.DATE));
			long cur_temp = cur_millis - (cur_millis % MILLIS_OF_DAY);
			String weight = c.getString(c.getColumnIndexOrThrow(Data.WEIGHT));
			String repeats = c.getString(c.getColumnIndexOrThrow(Data.REPEATS));
			String relax = c.getString(c.getColumnIndexOrThrow(Data.RELAX_TIME));
			CharSequence dt = null;
			
			TextView tvHeader = (TextView)v.findViewById(R.id.header);
			if( tvHeader != null ) {
				if( cur_temp != prev_millis ) {
					dt = DateFormat.format(DATE_FORMAT, cur_millis);
					tvHeader.setText(dt);
					tvHeader.setVisibility(View.VISIBLE);
				}
				else
					tvHeader.setVisibility(View.GONE);
			}
			
			TextView tvTime = (TextView)v.findViewById(R.id.time);
			if( tvTime != null ) {
				dt = DateFormat.format(TIME_FORMAT, cur_millis);
				tvTime.setText(dt);
			}
			
			TextView tvWeight = (TextView)v.findViewById(R.id.weight);
			if(tvWeight != null) {
				tvWeight.setTextColor(getResources().getColor(android.R.color.darker_gray));
				tvWeight.setText(weight);
			}
			
			TextView tvRepeats = (TextView)v.findViewById(R.id.repeats);
			if( tvRepeats != null )
				tvRepeats.setText(repeats);
			
			TextView tvRelax = (TextView)v.findViewById(R.id.relax);
			if(tvRelax != null)
				tvRelax.setText(relax);
		}
		
	}
}


