package com.tuoved.app.ui;

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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tuoved.app.R;
import com.tuoved.app.provider.ProviderMetaData.Data;
import com.tuoved.app.provider.ProviderMetaData.Labels;
import com.tuoved.app.ui.ExerciseListFragment.OnExercisePopupMenuListener;
import com.tuoved.app.utils.ExerciseData;
import com.tuoved.app.utils.Utils;

public class ExerciseActivity extends FragmentActivity implements 
OnClickListener, LoaderCallbacks<Cursor>, OnFocusChangeListener, OnExercisePopupMenuListener {
	private static final String TAG = "ExerciseActivity";
	private static final String TAG_DIALOG = "dialog";

	private static ExercisePagerAdapter mPagerAdapter;
	private static LoaderManager mLoaderManager;
	private static Ringtone mRingtone;
	private static RingtoneManager mRingtonManager;
	private static Vibrator mVibrator;
	private static PowerManager mPowerManager;
	private static PowerManager.WakeLock mWakeLock;

	private static final String SETTINGS_FILE = "settings";
	private static final String IS_STARTED = "is_started";
	private static final long MILLIS_OF_TWO_HOURS = 2*60*60*1000;
	private static final int ID_LOADER = 0;
	private static final long VIBR_MILLIS_SHORT = 20;
	private static long mLabelId = 0;
	private static String mTitle;
	private static Button btn_start;
	private static TextView text_timer;
	private static boolean is_started = false;
	private static CountDownTimer timer;
	private static ViewPager mPager;
	private static PagerTabStrip mPagerTabStrip;
	private static LinearLayout mLayoutHeader;

	private EditText etRelax, etRepeats, etWeight;
	private ExerciseData temp_data = new ExerciseData();
	private ExerciseData data;
	private int last_count_approach;
	private int last_count_training;
	private long last_date;
	private static Uri lastInsertedUri;
	private SharedPreferences settings;


	// --------------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate ( savedInstanceState );
		Log.d ( TAG, "onCreate: ");
		setContentView (R.layout.activity_exercise );
		setupActionBar();
		getViewFromId();
		registerListeners();
		loadSettings();
		setLabel(getIntent());
		fillData();
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
	// --------------------------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		if(timer!=null)
			timer.cancel();
		mLoaderManager.destroyLoader(ID_LOADER);
		super.onDestroy();
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public void onBackPressed() {
		if( is_started ) {
			cancelTimer();
			if(lastInsertedUri != null)
				removeExercise(ExerciseActivity.this, lastInsertedUri);
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
	private void getViewFromId() {
		// Initialization layout variables
		btn_start = (Button) findViewById ( R.id.button_Start );
		etRelax = (EditText) findViewById ( R.id.etRelax );
		etRepeats = (EditText) findViewById ( R.id.etRepeats );
		etWeight = (EditText) findViewById ( R.id.etWeight );
		text_timer = (TextView) findViewById ( R.id.timerView );
		text_timer.setVisibility(View.GONE);
		mPager = (ViewPager)findViewById(R.id.pager);
		mPagerTabStrip = (PagerTabStrip)findViewById(R.id.pagerTabStrip);
		mLayoutHeader = (LinearLayout)findViewById(R.id.layout_header);
	}
	//--------------------------------------------------------------------------
	private void setLabel(Intent intent){
		if( intent == null ) {
			this.setTitle ( mTitle );
			return;
		}
		mLabelId = intent.getLongExtra( LabelsFragment.EXTRA_ID_EXERCISE, 0 );
		String pathSegment = String.valueOf(mLabelId);
		final Uri uri = Labels.CONTENT_URI.buildUpon().appendPath(pathSegment).build();
		Cursor cursor = getContentResolver().query(uri, null, null, null, null);
		if(cursor != null && cursor.moveToFirst()) {
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
		etRelax.setText ( Long.toString (temp_data.relax()));
		temp_data.setRepeats(settings.getInt ("RepeatNum", 10));
		etRepeats.setText ( Integer.toString (temp_data.repeats()));
		temp_data.setWeight(settings.getFloat ("Weight", 15));
		etWeight.setText ( Float.toString ( temp_data.weight()));
	}

	//--------------------------------------------------------------------------
	private void fillData() {
		mLoaderManager = getSupportLoaderManager();
		mLoaderManager.initLoader(ID_LOADER, null, this);
	}

	//--------------------------------------------------------------------------
	private void registerListeners() {
		btn_start.setOnClickListener (this);
		btn_start.setOnFocusChangeListener(this);
		etWeight.addTextChangedListener(mWeightWatcher);
		etWeight.setOnFocusChangeListener(this);
		etWeight.setSelectAllOnFocus(true);
		etRepeats.addTextChangedListener ( mRepeatWatcher );
		etRepeats.setOnFocusChangeListener(this);
		etRepeats.setSelectAllOnFocus(true);
		etRelax.addTextChangedListener ( mRelaxWatcher );		
		etRelax.setOnFocusChangeListener(this);
		etRelax.setSelectAllOnFocus(true);
	}

	// -------------------------------------------------------------------------
	private TextWatcher mWeightWatcher = new TextWatcher ()	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count)
		{
			if(temp_data==null) 
				return;
			try	{
				temp_data.setWeight(Float.valueOf(s.toString ()));
			}
			catch (NumberFormatException e)	{
				temp_data.setWeight(0);
			} finally {
				savePreferences("Weight", temp_data.weight());
			}
		}

		@Override
		public void afterTextChanged(Editable s) {}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}
	};
			
	// -------------------------------------------------------------------------
	private TextWatcher mRelaxWatcher = new TextWatcher ()	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count)
		{
			if(temp_data==null) 
				return;
				try	{
					temp_data.setRelax(Long.valueOf(s.toString ()));
				} catch (NumberFormatException e) {
					temp_data.setRelax(30);
				} finally {
					savePreferences("Relax", temp_data.relax());
				}
		}
		
		@Override
		public void afterTextChanged(Editable s) {}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}
	};
	
	// -------------------------------------------------------------------------
	private TextWatcher mRepeatWatcher = new TextWatcher ()	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count)
		{
			if(temp_data==null) 
				return;
				try	{
					temp_data.setRepeats(Integer.valueOf(s.toString ()));
				} catch (NumberFormatException e)	{
					temp_data.setRepeats(0);
				} finally {
					savePreferences( "RepeatNum", temp_data.repeats());
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
		setEditor.putInt (key, value);
		setEditor.commit();
	}

	// -------------------------------------------------------------------------
	void savePreferences(String key, float value) {
		SharedPreferences settings = getSharedPreferences (
				SETTINGS_FILE, MODE_PRIVATE );
		SharedPreferences.Editor setEditor = settings.edit ();
		setEditor.putFloat(key, value);
		setEditor.commit();
	}

	// -------------------------------------------------------------------------
	void savePreferences(String key, long value) {
		SharedPreferences settings = getSharedPreferences (
				SETTINGS_FILE, MODE_PRIVATE );
		SharedPreferences.Editor setEditor = settings.edit ();
		setEditor.putLong(key, value);
		setEditor.commit();
	}

	// -------------------------------------------------------------------------
	public void onFocusChange(View v, boolean hasFocus) {
		switch( v.getId() )
		{
		case R.id.etWeight:
			etWeight.setSelection(0, etWeight.getText().length() );
			break;
		case R.id.etRepeats:
			etRepeats.setSelection(0, etRepeats.getText().length() );
			break;
		case R.id.etRelax:
			etRelax.setSelection(0, etRelax.getText().length());
			break;
		}
	}

	// -------------------------------------------------------------------------
	private void startTimer() {

		final long relax_time = temp_data.relax();
		final int procOfTime = (int)(0.1 * (double)relax_time);
		final int redColor = getResources().getColor(R.color.red );
		final int greenColor = getResources().getColor(R.color.green);

		text_timer.setVisibility(View.VISIBLE);
		is_started = true;
		mVibrator.vibrate(VIBR_MILLIS_SHORT);
		btn_start.setText (R.string.continue_);
		text_timer.setText (null);
		etRelax.clearFocus();
		etRepeats.clearFocus();
		etWeight.clearFocus();

		if(relax_time == 0) {
			onClick( btn_start );
			return;
		}
		setPagerCurrentPage(0);
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
		Utils.hideKeyboard(this, v);
		switch (v.getId ())
		{
		case R.id.button_Start:
			if( !is_started ) {
				if(!checkEnteredData()) {
					Toast.makeText(this, R.string.check_entered_data, Toast.LENGTH_SHORT).show();
					return;
				}
				startTimer();
				addExercise(ExerciseActivity.this);
			}
			else {
				cancelTimer();
				updateExercise(ExerciseActivity.this);
			}
			break;
		default:
			return;
		}
	}

	// -------------------------------------------------------------------------
	private boolean checkEnteredData() {
		boolean isValidData = false;
		if(etRelax!=null && etRepeats!=null && etWeight!=null)
			isValidData = (etRelax.length()!=0 && etRepeats.length()!=0 && etWeight.length()!=0) ? true : false;
		return isValidData;
	}

	// -------------------------------------------------------------------------
	private void cancelTimer() {
		text_timer.setVisibility(View.GONE);
		is_started = false;
		btn_start.setText (R.string.add);
		text_timer.setText ("");
		if(timer != null)
			timer.cancel ();
		if(mWakeLock.isHeld())
			mWakeLock.release();
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
	private void addExercise( Context context )
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
		cv.put( Data.DATE, cur_date_time );
		cv.put ( Data.WEIGHT, (Float)data.weight() );
		cv.put ( Data.REPEATS, (Integer)data.repeats() );
		cv.put ( Data.RELAX_TIME, (Long)data.relax() );
		cv.put( Data.LABEL_ID, mLabelId );
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
	private void removeExercise( Context context, Uri delUri ) {
		ContentResolver cr = context.getContentResolver ();
		int trainingToUpdate = getCountTraining(cr, delUri);
		boolean isDeleted = cr.delete (delUri, null, null) > 0 ? true : false;
		if(isDeleted) {
			if(delUri.equals(lastInsertedUri))
				lastInsertedUri = null;
			else
				updateAfterDelete(cr, delUri, trainingToUpdate);
		}
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}

	// -------------------------------------------------------------------------
	private void updateExercise(Context context) {
		if(lastInsertedUri == null) 
			return;
		ContentValues cv = new ContentValues ();
		cv.put ( Data.RELAX_TIME, data.relax() );
		ContentResolver cr = context.getContentResolver ();
		int num_row = cr.update(lastInsertedUri, cv, null, null);
		Log.d ( TAG, "Updated rows " + num_row );
		mLoaderManager.restartLoader(ID_LOADER, null, this);
	}

	// -------------------------------------------------------------------------
	private static int getCountTraining (ContentResolver cr, Uri uri) {
		String[] proj = new String[]{Data.COUNT_TRAINING};
		Cursor c = cr.query(uri, proj, null, null, null);
		int count_training = 0;
		if(c!=null && c.moveToFirst()) {
			count_training = c.getInt(c.getColumnIndex(Data.COUNT_TRAINING));
			c.close();
			c = null;
		}
		return count_training;
	}

	// -------------------------------------------------------------------------
	private static void updateAfterDelete(ContentResolver cr, Uri delUri, int trainingToUpdate ) {
		Cursor c = null;
		String[] proj = new String[]{Data._ID, Data.COUNT_APPROACH};
		String selection = Data.COUNT_TRAINING + "=?" + " AND " + Data.LABEL_ID + "=?";
		String []selArgs = new String []{String.valueOf(trainingToUpdate), String.valueOf(mLabelId) };
		String sortOrder = Data.COUNT_APPROACH + " ASC";
		c = cr.query(Data.CONTENT_URI, proj, selection, selArgs, sortOrder);
		if(c!=null) {
			boolean needToUpdateTraining = (c.getCount() == 0) ? true : false;
			if(needToUpdateTraining)
				updateCountTraining(cr, trainingToUpdate);
			else {
				c.moveToFirst();
				int cur_approach = 0;
				do {
					int read_approach = c.getInt(c.getColumnIndex(Data.COUNT_APPROACH));
					int id = c.getInt(c.getColumnIndex(Data._ID));
					boolean needToUpdate = (++cur_approach == read_approach) ? false : true;
					if(needToUpdate) {
						ContentValues values = new ContentValues();
						values.put(Data.COUNT_APPROACH, cur_approach);
						cr.update(Data.buildDataUriWithId(id), values, null, null);
					}
				} while(c.moveToNext());
				c.close();
				c = null;
			}
		}
	}

	// -------------------------------------------------------------------------
	private static void updateCountTraining(ContentResolver cr, int trainingToUpdate) {
		String[] proj = new String[]{Data._ID, Data.COUNT_TRAINING, Data.DATE};
		String selection = Data.LABEL_ID + "=?";
		String[] selArgs = new String []{String.valueOf(mLabelId) };
		String sortOrder = Data.COUNT_TRAINING + " ASC";
		Cursor c = cr.query(Data.CONTENT_URI, proj, selection, selArgs, sortOrder);
		if(c!=null && c.moveToLast()) {
			int read_training = c.getInt(c.getColumnIndex(Data.COUNT_TRAINING));
			boolean isLastTraining = (read_training==trainingToUpdate) ? true : false;
			if(!isLastTraining) {
				c.moveToFirst();
				int cur_training = 1;
				int id = 0;
				long prev_date = 0;
				do {
					read_training = c.getInt(c.getColumnIndex(Data.COUNT_TRAINING));
					id = c.getInt(c.getColumnIndex(Data._ID));
					long cur_date = c.getLong(c.getColumnIndex(Data.DATE));
					if((prev_date-cur_date) >= MILLIS_OF_TWO_HOURS)
						++cur_training;
					boolean needToUpdate = (cur_training != read_training) ? true : false;
					if(needToUpdate) {
						ContentValues values = new ContentValues();
						values.put(Data.COUNT_TRAINING, cur_training);
						cr.update(Data.buildDataUriWithId(id), values, null, null);
					}
					prev_date = cur_date;
				} while(c.moveToNext());
			}
			c.close();
			c = null;
		}
	}

	// -------------------------------------------------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int loader,
			Bundle args) {
		Uri uri = Labels.CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(mLabelId))
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
		return new CursorLoader(getApplicationContext(), uri, projection, null, null, null);
	}

	// -------------------------------------------------------------------------
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
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
		boolean isEmpty = data.getCount() == 0;
		mPagerTabStrip.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
		mLayoutHeader.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
		int prevPos = mPager.getCurrentItem();
		mPagerAdapter = new ExercisePagerAdapter(getSupportFragmentManager(), last_count_training);
		mPager.setAdapter(mPagerAdapter);
		setPagerCurrentPage(prevPos);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onChangeData(long id) {
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_DIALOG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		DialogFragment dialog = ExerciseDataChangeDialog.newInstance(id);
		dialog.show(ft, TAG_DIALOG);
	}
	
	// -------------------------------------------------------------------------
	@Override
	public void onDeleteData(long id) {
		Uri delUri = Data.buildDataUriWithId(id);
		boolean isLastInsertedUri = delUri.equals(lastInsertedUri);
		if(is_started && isLastInsertedUri)
			cancelTimer();
		removeExercise(this, delUri);
	}
	
	// -------------------------------------------------------------------------
	private void setPagerCurrentPage(final int pos) {
		mPager.post(new Runnable() {
			@Override
			public void run() {
					mPager.setCurrentItem(pos);
			}
		});
	}

	// -------------------------------------------------------------------------
	public static class ExerciseDataChangeDialog extends DialogFragment implements OnClickListener, LoaderCallbacks<Cursor>, OnFocusChangeListener {
		// constants
		private static final String ID_DATA_TO_CHANGE = "id";
		private static final int ID_LOADER_CHANGE = 10;
		private static final String TAG = "ExerciseDataChangeDialog";
		// Views
		private View mView;
		private EditText etWeight;
		private EditText etRepeats;
		private EditText etRelax;
		private Button btnChange;
		private Uri mUri = null;
		ExerciseData loadedData, changedData;

		// -------------------------------------------------------------------------
		public ExerciseDataChangeDialog() {	}

		// -------------------------------------------------------------------------
		public static ExerciseDataChangeDialog newInstance(long id) {
			ExerciseDataChangeDialog dlg = new ExerciseDataChangeDialog();
			Bundle args = new Bundle();
			args.putLong(ID_DATA_TO_CHANGE, id);
			dlg.setArguments(args);
			return dlg;
		}

		// -------------------------------------------------------------------------
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mView = inflater.inflate(R.layout.exercise_data_dialog, container, false);
			etWeight = (EditText)mView.findViewById(R.id.etWeight);
			etWeight.setOnFocusChangeListener(this);
			etWeight.setSelectAllOnFocus(true);
			etRepeats = (EditText)mView.findViewById(R.id.etRepeats);
			etRepeats.setOnFocusChangeListener(this);
			etRepeats.setSelectAllOnFocus(true);
			etRelax = (EditText)mView.findViewById(R.id.etRelax);
			etRelax.setOnFocusChangeListener(this);
			etRelax.setSelectAllOnFocus(true);
			btnChange = (Button)mView.findViewById(R.id.btnChange);
			btnChange.setOnClickListener(this);
			return mView;
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
		public void onClick(View v) {
			Utils.hideKeyboard(getActivity(), v);
			switch(v.getId()) {
			case R.id.btnChange:
				updateData(getActivity());
				break;
			}
			dismiss();
		}

		// -------------------------------------------------------------------------
		private void updateData(Context context) {
			if(!checkEnteredData()) {
				Toast.makeText(getActivity(), R.string.check_entered_data, Toast.LENGTH_SHORT).show();
				return;
			}
			if(!isNeedToUpdate())
				return;
			ContentResolver cr = context.getContentResolver();
			ContentValues cv = new ContentValues();
			cv.put(Data.WEIGHT, Float.valueOf(changedData.weight()));
			cv.put(Data.REPEATS, Integer.valueOf(changedData.repeats()));
			cv.put(Data.RELAX_TIME, Long.valueOf(changedData.relax()));
			if(cr.update(mUri, cv, null, null) > 0) {
				mLoaderManager.restartLoader(ID_LOADER, null, (ExerciseActivity)getActivity());
				Toast.makeText(getActivity(), R.string.data_successfully_updated, Toast.LENGTH_SHORT).show();
			}
		}

		// -------------------------------------------------------------------------
		private boolean checkEnteredData() {
			boolean isChecked = (etWeight.length() !=0 && etRepeats.length()!=0 && etRelax.length()!=0) ? true : false;
			return isChecked;
		}

		// -------------------------------------------------------------------------
		private boolean isNeedToUpdate() {
			float weight = Float.valueOf(etWeight.getText().toString());
			int repeats = Integer.valueOf(etRepeats.getText().toString());
			long relax = Long.valueOf(etRelax.getText().toString());
			changedData = new ExerciseData( weight, repeats, relax);
			return !changedData.equals(loadedData);
		}

		// -------------------------------------------------------------------------
		@Override
		public Loader<Cursor> onCreateLoader(int id_loader, Bundle bundle) {
			if(bundle==null && id_loader!=ID_LOADER_CHANGE)
				return null;
			long id_data = 0;
			id_data = bundle.getLong(ID_DATA_TO_CHANGE);
			mUri = Data.buildDataUriWithId(id_data);
			String [] projection = {Data.WEIGHT, Data.REPEATS, Data.RELAX_TIME};
			return new CursorLoader(getActivity(), mUri, projection, null, null, null);
		}

		// -------------------------------------------------------------------------
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
			if(loader!=null && c!=null) {
				if(c.moveToFirst()) {
					loadedData = new ExerciseData(c.getFloat(c.getColumnIndex(Data.WEIGHT)),
							c.getInt(c.getColumnIndex(Data.REPEATS)),
							c.getLong(c.getColumnIndex(Data.RELAX_TIME)));
					etWeight.setText(String.valueOf(loadedData.weight()));
					etRepeats.setText(String.valueOf(loadedData.repeats()));
					etRelax.setText(String.valueOf(loadedData.relax()));
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}

		// -------------------------------------------------------------------------
		@Override
		public void onDestroyView() {
			Log.d(TAG, "OnDestroyView dialog");
			mView = null;
			super.onDestroyView();
		}

		// -------------------------------------------------------------------------
		@Override
		public void onDestroy() {
			Log.d(TAG, "OnDestroy dialog");
			getLoaderManager().destroyLoader(ID_LOADER_CHANGE);
			super.onDestroy();
		}

		// -------------------------------------------------------------------------
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			switch(v.getId()) {
			case R.id.etWeight:
				etWeight.setSelection(0, etWeight.length());
				break;
			case R.id.etRepeats:
				etRepeats.setSelection(0, etRepeats.length());
				break;
			case R.id.etRelax:
				etRelax.setSelection(0, etRelax.length());
				break;
			default:
				return;
			}
		}
	}

	// -------------------------------------------------------------------------
	private class ExercisePagerAdapter extends FragmentStatePagerAdapter {

		private int mCount;

		public ExercisePagerAdapter(FragmentManager fm, int page_count) {
			super(fm);
			mCount = page_count;
		}

		@Override
		public Fragment getItem(int pos) {
			return ExerciseListFragment.newInstance(pos, last_count_training, mLabelId);
		}

		@Override
		public int getCount() {
			return mCount;
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
			return "  "+(last_count_training-pos)+"  ";
		}
		
	}	
}


