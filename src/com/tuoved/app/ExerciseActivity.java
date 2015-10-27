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
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
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
	
	private static final String SETTINGS_FILE = "settings";
	private static final String IS_STARTED = "is_started";
	private static final int ACTION_DELETE = 0;
	private final long MILLIS_OF_TWO_HOURS = 2*60*60*1000;
	private static final int ID_LOADER = 0;
	private static final long VIBR_MILLIS_SHORT = 20;
	private static long mLabelRowId = 0;
	private static String mTitle;
	private static Button btn_start;
	private static TextView text_timer;
	private static ListView mListView;
	private static boolean is_started = false;
	private static CountDownTimer timer;
	
	private EditText edit_relax_time, edit_repeats, edit_weight;
	private ExerciseData temp_data = new ExerciseData();
	private ExerciseData data;
	private int last_count_approach;
	private int last_count_training;
	private long last_date;
	private Uri lastInsertedUri;
	private SharedPreferences settings;
	

	// --------------------------------------------------------------------------------------------
	// @SuppressLint("ResourceAsColor")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate ( savedInstanceState );
		Log.d ( TAG, "onCreate: ");
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
			int resId = is_started ? R.string.continue_ : R.string.add;
			btn_start.setText(resId);
		}
		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0, ACTION_DELETE, 0, R.string.delete);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch(item.getItemId()) {
		case ACTION_DELETE:
			removeExercise(this, info.id);
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
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
			btn_start.setText (R.string.add);
			text_timer.setText ("");
			if( timer != null )
				timer.cancel();
			if( lastInsertedUri != null )
				removeExercise(ExerciseActivity.this, lastInsertedUri);
			if(text_timer.isShown())
				text_timer.setVisibility(View.GONE);
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
		text_timer.setVisibility(View.GONE);
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
		mLabelRowId = intent.getLongExtra( MainActivity.EXTRA_ID_EXERCISE, 0 );
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
		settings = getSharedPreferences (SETTINGS_FILE, MODE_PRIVATE);
		temp_data.setRelax(settings.getLong ("Relax", 30));
		edit_relax_time.setText ( Long.toString (temp_data.getTime()));
		temp_data.setRepeats(settings.getInt ("RepeatNum", 10));
		edit_repeats.setText ( Integer.toString (temp_data.getRepeats()));
		temp_data.setWeight(settings.getFloat ("Weight", 15));
		edit_weight.setText ( Float.toString ( temp_data.getWeight()));
	}
	
	//--------------------------------------------------------------------------
	private void fillData() {
		mLoaderManager = getSupportLoaderManager();
		mLoaderManager.initLoader(ID_LOADER, null, this);
		mAdapter = new MyAdapter(this, R.layout.row_item, null);
		mListView.setAdapter(mAdapter);
	}
	
	//--------------------------------------------------------------------------
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
			if(temp_data!=null) {
				try	{
					temp_data.setWeight(Float.parseFloat (s.toString ()));
				}
				catch (NumberFormatException e)	{
					temp_data.setWeight( 0 );
				} finally {
					savePreferences("Weight", temp_data.getWeight());
				}
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
			if(temp_data!=null) {
				try	{
					temp_data.setRelax(Long.parseLong (s.toString ()));
				}
				catch (NumberFormatException e)	{
					temp_data.setRelax(30);
				} finally {
					savePreferences("Relax", temp_data.getTime());
				}
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
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if(temp_data!=null) {
				try	{
					temp_data.setRepeats(Integer.parseInt (s.toString ()));
				}
				catch (NumberFormatException e)	{
					temp_data.setRepeats(0);
				} finally {
					savePreferences( "RepeatNum", temp_data.getRepeats());
				}
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
	void savePreferences(String key, long value) {
		SharedPreferences settings = getSharedPreferences (
				SETTINGS_FILE, MODE_PRIVATE );
		SharedPreferences.Editor setEditor = settings.edit ();
		setEditor.putLong( key, value );
		setEditor.commit();
	}
	
	// -------------------------------------------------------------------------
	public void onFocusChange(View v, boolean hasFocus) {
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
		
		final long relax_time = temp_data.getTime();
		final int procOfTime = (int)(0.1 * (double)relax_time);
		final int redColor = getResources().getColor(R.color.red );
		final int greenColor = getResources().getColor(R.color.green);
		if(relax_time == 0) {
			onClick( btn_start );
			return;
		}
		mWakeLock.acquire();
		data = new ExerciseData(temp_data);
		data.setRelax(0);
		timer = new CountDownTimer ( 1000 * ( relax_time ), 500 ) {
			StringBuilder builder = new StringBuilder();

			@Override			
			public void onTick(long millisUntilFinished) {
				builder.delete(0, builder.length());
				long sec = millisUntilFinished / 1000;
				long minutes = sec/60;
				long seconds = sec%60;
				data.setRelax(relax_time - sec);
				if(minutes < 10)
					builder.append(0);
				builder.append(minutes).append(" : ");
				if(seconds < 10)
					builder.append(0);
				builder.append(seconds);
				text_timer.setTextColor (sec <= procOfTime ? redColor : greenColor );
				text_timer.setText (builder.toString());
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
				catch( NullPointerException e ) {
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
				text_timer.setVisibility(View.VISIBLE);
				is_started = true;
				mVibrator.vibrate(VIBR_MILLIS_SHORT);
				btn_start.setText ("Продолжить");
				text_timer.setText ("");
				edit_relax_time.clearFocus();
				edit_repeats.clearFocus();
				edit_weight.clearFocus();
				startTimer();
				addExercise(ExerciseActivity.this);
			}
			else {
				text_timer.setVisibility(View.GONE);
				is_started = false;
				btn_start.setText (R.string.add);
				text_timer.setText ("");
				updateExercise(ExerciseActivity.this);
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
	public void addExercise( Context context )
	{
		ContentValues cv = new ContentValues ();
		Long cur_date_time = Long.valueOf ( System.currentTimeMillis () );
		int cur_approach;
		int cur_training;
		if((cur_date_time-last_date) > MILLIS_OF_TWO_HOURS ) {
			cur_approach = 1;
			cur_training = last_count_training + 1;
		}
		else {
			cur_approach = last_count_approach + 1;
			cur_training = last_count_training;
		}		
		cv.put(Data.DATE, cur_date_time);
		cv.put ( Data.WEIGHT, (Float)data.getWeight() );
		cv.put ( Data.REPEATS, (Integer)data.getRepeats() );
		cv.put ( Data.RELAX_TIME, (Long)data.getTime() );
		cv.put( Data.LABEL_ID, mLabelRowId );
		cv.put( Data.COUNT_APPROACH, cur_approach);
		cv.put( Data.COUNT_TRAINING, cur_training);
		ContentResolver cr = context.getContentResolver ();
		Uri uri = Data.CONTENT_URI;
		Log.d ( TAG, "Exercise insert uri: " + uri );
		lastInsertedUri = cr.insert ( uri, cv );
		Log.d ( TAG, "Inserted URI: " + lastInsertedUri );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	public void removeExercise( Context context, long id ) {
		ContentResolver cr = context.getContentResolver ();
		Uri uri = Data.CONTENT_URI;
		Uri delUri = uri.buildUpon().appendPath(Long.toString (id)).build();
		Log.d ( TAG, "Del URI: " + delUri );
		int number = cr.delete ( delUri, null, null );
		Log.d ( TAG, "Deleted number: " + number );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	public void removeExercise( Context context, Uri delUri ) {
		ContentResolver cr = context.getContentResolver ();
		Log.d ( TAG, "Del URI: " + delUri );
		int number = cr.delete ( delUri, null, null );
		Log.d ( TAG, "Deleted number: " + number );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}
	
	// -------------------------------------------------------------------------
	public void updateExercise( Context context ) {
		ContentValues cv = new ContentValues ();
		cv.put ( Data.RELAX_TIME, (Long)data.getTime() );
		ContentResolver cr = context.getContentResolver ();
		int num_row = cr.update(lastInsertedUri, cv, null, null);
		Log.d ( TAG, "Updated rows " + num_row );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
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
				Data.LABEL_ID,
				Data.COUNT_APPROACH,
				Data.COUNT_TRAINING
		};
		return new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onLoadFinished(Loader<Cursor> loader,
			Cursor data) {
		mAdapter.swapCursor(data);
		if(data == null)
			return;
		final int pos = data.getCount()-1;
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
		if(data.moveToLast()) {
			last_count_approach = data.getInt(data.getColumnIndexOrThrow(Data.COUNT_APPROACH));
			last_count_training = data.getInt(data.getColumnIndexOrThrow(Data.COUNT_TRAINING));
			last_date = data.getLong(data.getColumnIndexOrThrow(Data.DATE));
		}
		else {
			last_count_approach = 0;
			last_count_training = 1;
			last_date = System.currentTimeMillis();
		}
	}
	// -------------------------------------------------------------------------
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	private class ViewHolder{
		TextView tvHeader;
		TextView tvTime;
		TextView tvWeight;
		TextView tvRepeats;
		TextView tvRelax;
	}
	
	// -------------------------------------------------------------------------
	private class MyAdapter extends CursorAdapter {
		private final CharSequence DATE_FORMAT = "dd-MM-yy (EEE) kk:mm";
		
		private LayoutInflater mInflater;
		private int mLayout;
		
		public MyAdapter(Context context, int layout, Cursor c) {
			super(context, c, false);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
		}

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
		
		@Override
		public void bindView(View v, Context context, Cursor c) {
			ViewHolder holder = (ViewHolder) v.getTag();
			int pos_cur = c.getPosition();
			long time_prev_ms = 0;
			int pos_prev = pos_cur-1;
			if(pos_prev >= 0) {
				c.moveToPosition(pos_prev);
				time_prev_ms = c.getLong(c.getColumnIndexOrThrow(Data.DATE));
			}
			c.moveToPosition(pos_cur);
			long time_cur_ms = c.getLong(c.getColumnIndexOrThrow(Data.DATE));

			CharSequence dt = null;
			if(holder.tvHeader != null) {
				if( (time_cur_ms-time_prev_ms)>= MILLIS_OF_TWO_HOURS ) {
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
	
	// -------------------------------------------------------------------------
	private class ExerciseData {
		private int repeats;
		private long time;
		private float weight;
		
		public ExerciseData(float weight, int repeats, long time){
			this.weight = weight;
			this.repeats = repeats;
			this.time = time;
		}
		
		public ExerciseData(ExerciseData data) {
			this(data.getWeight(), data.getRepeats(), data.getTime());
		}
		
		public ExerciseData() {
			this.weight = 0;
			this.repeats = 0;
			this.time = 0;
		}
		
		public void setWeight(float weight) {
			this.weight = weight;
		}
		
		public float getWeight() {
			return this.weight;
		}
		
		public void setRepeats(int repeats) {
			this.repeats = repeats;
		}
		
		public int getRepeats() {
			return this.repeats;
		}
		
		public void setRelax(long time) {
			this.time = time;
		}
		
		public long getTime() {
			return this.time;
		}
	}
}


