package com.tuoved.app;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.tuoved.app.ProviderMetaData.Data;
import com.tuoved.app.ProviderMetaData.Labels;


public class ExerciseProvider extends ContentProvider
{
	private static final String TAG = "ExerciseProvider";

	// Создание карт проекций
	private static HashMap<String, String> sProjectionMapLabels;
	static {
		sProjectionMapLabels = new HashMap<String, String>();
		sProjectionMapLabels.put(Labels._ID, Labels.TABLE_NAME +  "." + Labels._ID);
		sProjectionMapLabels.put(Labels.NAME, Labels.TABLE_NAME +  "." + Labels.NAME);
	}
	private static HashMap<String, String> sProjectionMapData;	
	static {
		sProjectionMapData = new HashMap<String, String> ();
		sProjectionMapData.put(Data._ID, Data.TABLE_NAME + "." + Data._ID);
		sProjectionMapData.put(Data.WEIGHT, Data.TABLE_NAME + "." + Data.WEIGHT);
		sProjectionMapData.put(Data.REPEATS, Data.TABLE_NAME + "." + Data.REPEATS);
		sProjectionMapData.put(Data.RELAX_TIME, Data.TABLE_NAME + "." + Data.RELAX_TIME);
		sProjectionMapData.put(Data.DATE, Data.TABLE_NAME + "." + Data.DATE );
		sProjectionMapData.put(Data.LABEL_ID, Data.TABLE_NAME + "." + Data.LABEL_ID);
	}

	// Создание Uri
	private static UriMatcher sUriMatcher;
	
	private static final int LABELS = 100;
	private static final int LABELS_ID = 101;
	private static final int LABELS_ID_DATA = 102;
	
	private static final int DATA = 200;
	private static final int DATA_ID = 201;

	static
	{
		sUriMatcher = new UriMatcher ( UriMatcher.NO_MATCH );
		sUriMatcher.addURI(ProviderMetaData.AUTHORITY, "labels", LABELS);
		sUriMatcher.addURI(ProviderMetaData.AUTHORITY, "labels/#", LABELS_ID);
		sUriMatcher.addURI(ProviderMetaData.AUTHORITY, "labels/#/data", LABELS_ID_DATA);
		sUriMatcher.addURI(ProviderMetaData.AUTHORITY, "data", DATA);
		sUriMatcher.addURI(ProviderMetaData.AUTHORITY, "data/#", DATA_ID);
	}
	
	private interface Triggers {
		String EXERCISE_DELETE = "exercise_delete";
	}

	// Настройка и создание базы данных
	// --------------------------------------------------------------------------
	// Вспомогательный класс для открытия, создания и обновления файла базы
	// данных
	private static class DataBaseHelper extends SQLiteOpenHelper
	{
		
		private static final String CREATE_LABELS_TABLE = "CREATE TABLE "
				+ Labels.TABLE_NAME + " ("
				+ Labels._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Labels.NAME + " TEXT NOT NULL);";
		
		private static final String REFERENCES_LABELS_ID = 
				"REFERENCES " + Labels.TABLE_NAME + " (" + Labels._ID + ")";
		
		private static final String CREATE_DATA_TABLE = "CREATE TABLE "
			+ Data.TABLE_NAME + " ("
			+ Data._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ Data.WEIGHT + " REAL,"
			+ Data.REPEATS + " INTEGER,"
			+ Data.RELAX_TIME + " INTEGER,"
			+ Data.DATE + " LONG,"
			+ Data.LABEL_ID + " INTEGER NOT NULL " + REFERENCES_LABELS_ID + ");";
		
		private static final String CREATE_TRIGGER_DELETE_EXERCISES = "CREATE TRIGGER "
				+ Triggers.EXERCISE_DELETE + " AFTER DELETE ON " + Labels.TABLE_NAME
				+ " BEGIN DELETE FROM " + Data.TABLE_NAME + " WHERE " + " "
				+ Data.LABEL_ID + "=old." + Labels._ID + ";" + "END;";

		DataBaseHelper(Context context)
		{
			super ( context, ProviderMetaData.DATABASE_NAME, null,
				ProviderMetaData.DATABASE_VERSION );
		}

		// ------------------------------------------------------------------------
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			Log.d ( TAG, "inner onCreate called" );
			db.execSQL(CREATE_LABELS_TABLE);
			db.execSQL(CREATE_DATA_TABLE);
			db.execSQL(CREATE_TRIGGER_DELETE_EXERCISES);
		}

		// ------------------------------------------------------------------------
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d ( TAG, "inner onUpgrade called" );
			int version = oldVersion;
			switch( version ) {
			case ProviderMetaData.VER_LAUNCH:
				// Version 2 added trigger for deleting from table Data, 
				// which called deleting one or more items from table Labels
				db.execSQL(CREATE_TRIGGER_DELETE_EXERCISES);
				version = ProviderMetaData.VER_ADD_TRIGGER;
			}
			Log.d(TAG, "after upgrade logic, at version " + version);
			
			if( version != ProviderMetaData.DATABASE_VERSION ) {
				db.execSQL( "DROP TABLE IF EXISTS " + Labels.TABLE_NAME );
				db.execSQL ( "DROP TABLE IF EXISTS " + Data.TABLE_NAME );
				
				db.execSQL ( "DROP TRIGGER IF EXISTS " + Triggers.EXERCISE_DELETE );

				onCreate ( db );
			}
		}
		
	}

	private DataBaseHelper mOpenHelper;

	// ------------------------------------------------------------------------
	@Override
	public int delete(Uri uri, String where, String[] whereArgs)
	{
		// TODO Auto-generated method stub
		SQLiteDatabase db = mOpenHelper.getWritableDatabase ();
		int count = 0;
		String rowId = "";
		switch (sUriMatcher.match ( uri ))
		{
		case LABELS:
			count = db.delete(Labels.TABLE_NAME, where, whereArgs);
			break;
		case LABELS_ID:
			rowId = uri.getPathSegments().get(1);
			count = db.delete(Labels.TABLE_NAME,
					Labels._ID + " =" + rowId
					+ ( !TextUtils.isEmpty(where) ? " AND (" + where + ")" : "" ), 
					whereArgs);
			break;
		case DATA:
			count = db.delete ( Data.TABLE_NAME, where,	whereArgs );
			break;
		case DATA_ID:
			rowId = uri.getPathSegments ().get (1);
			count = db.delete ( Data.TABLE_NAME,
				Data._ID + "="	+ rowId
					+ ( !TextUtils.isEmpty ( where ) ? " AND (" + where + ')' : "" ),
					whereArgs );
			break;
		default:
			throw new IllegalArgumentException ( "Unknown URI" + uri );
		}
		getContext ().getContentResolver ().notifyChange ( uri, null );
		return count;
	}

	// ------------------------------------------------------------------------
	@Override
	public String getType(Uri uri)
	{
		// TODO Auto-generated method stub
		switch (sUriMatcher.match ( uri ))
		{
		case LABELS:
			return Labels.CONTENT_TYPE;
		case LABELS_ID:
			return Labels.CONTENT_ITEM_TYPE;
		case LABELS_ID_DATA:
			return Data.CONTENT_TYPE;
		case DATA:
			return Data.CONTENT_TYPE;
		case DATA_ID:
			return Data.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException ( "Unknown URI" + uri );
		}
	}

	// ------------------------------------------------------------------------
	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase ();
		final int match = sUriMatcher.match ( uri );
		long rowId = -1;
		switch(match){
		case LABELS: {
			rowId = db.insertOrThrow(Labels.TABLE_NAME, null, initialValues);
			getContext().getContentResolver().notifyChange(uri, null);
			return Labels.buildLabelsUriWithId(rowId);
		}
		case DATA: {
			Long now = Long.valueOf ( System.currentTimeMillis () );
			if( !initialValues.containsKey(Data.DATE) )
				initialValues.put(Data.DATE, now);
			rowId = db.insertOrThrow(Data.TABLE_NAME, null, initialValues );
			getContext ().getContentResolver ().notifyChange (uri, null);
			return Data.buildDataUriWithId(rowId);
		}
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri );
		}
	}

	// ------------------------------------------------------------------------
	@Override
	public boolean onCreate()
	{
		mOpenHelper = new DataBaseHelper ( getContext () );
		Log.d ( TAG, "main onCreate called" );
		return true;
	}

	// ------------------------------------------------------------------------
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
		String[] selectionArgs, String sortOrder)
	{
		// TODO Auto-generated method stub
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder ();
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase ();
		String orderBy = null;
		switch (sUriMatcher.match ( uri ))
		{
		case LABELS:
			qb.setTables(Labels.TABLE_NAME);
			qb.setProjectionMap(sProjectionMapLabels);
			orderBy = (TextUtils.isEmpty ( sortOrder )) ?
					Labels.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case LABELS_ID:
			qb.setTables(Labels.TABLE_NAME);
			qb.setProjectionMap(sProjectionMapLabels);
			qb.appendWhere( Labels._ID + "=" + uri.getPathSegments().get(1));
			break;
		case LABELS_ID_DATA:
			qb.setTables( ProviderMetaData.DATA_JOIN_LABELS );
			qb.appendWhere( Labels.TABLE_NAME + "." + Labels._ID + "=" + uri.getPathSegments().get(1));
			orderBy = (TextUtils.isEmpty ( sortOrder )) ?
					Data.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case DATA:
			qb.setTables ( Data.TABLE_NAME );
			qb.setProjectionMap ( sProjectionMapData );
			orderBy = (TextUtils.isEmpty ( sortOrder )) ?
				Data.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case DATA_ID:
			qb.setTables ( Data.TABLE_NAME );
			qb.setProjectionMap ( sProjectionMapData );
			qb.appendWhere ( Data._ID + "="
				+ uri.getPathSegments ().get (1) );
			break;
		default:
			throw new IllegalArgumentException ( "Unknown URI" + uri );
		}
		// Получение базы данных и выполнение запроса
		
		Cursor cursor = qb.query ( db, projection, selection, selectionArgs,
			null, null, orderBy );

		// Указание курсору на URi за которым необходимо наблюдать,
		// чтобы не пропустить изменение исходных данных
		cursor.setNotificationUri ( getContext ().getContentResolver (), uri );
		return cursor;
	}

	// ------------------------------------------------------------------------
	@Override
	public int update(Uri uri, ContentValues values, String where,
		String[] whereArgs)
	{
		// TODO Auto-generated method stub
		SQLiteDatabase db = mOpenHelper.getWritableDatabase ();
		int count = 0;
		String rowId = "";
		switch (sUriMatcher.match ( uri ))
		{
		case LABELS: {
			count = db.update(Labels.TABLE_NAME, values, where, whereArgs);
			break;
		}
		case LABELS_ID: {
			rowId = uri.getPathSegments ().get (1);
			count = db.update ( Labels.TABLE_NAME, values,
				Labels._ID + "=" + rowId
					+ ( !TextUtils.isEmpty ( where ) ? " AND (" + where + ')'
						: "" ), whereArgs );
			break;
		}
		case DATA: {
			count = db.update ( Data.TABLE_NAME, values, where, whereArgs );
			break;
		}
		case DATA_ID: {
			rowId = uri.getPathSegments ().get (1);
			count = db.update ( Data.TABLE_NAME, values,
				Data._ID + "=" + rowId
					+ ( !TextUtils.isEmpty ( where ) ? " AND (" + where + ')'
						: "" ), whereArgs );
			break;
		}
		default:
			throw new IllegalArgumentException ( "Unknown URI" + uri );
		}
		getContext ().getContentResolver ().notifyChange ( uri, null );
		return count;
	}

}
