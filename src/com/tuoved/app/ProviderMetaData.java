package com.tuoved.app;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class ProviderMetaData
{
	private ProviderMetaData(){ }
	
	public static final String AUTHORITY = "com.tuoved.app.provider.ExerciseProvider";
	public static final Uri BASE_CONTENT_URI =  Uri.parse ( "content://" + AUTHORITY );
	public static final String DATABASE_NAME = "exercise.db";
	public static final int VER_FIRST = 1;
	public static final int VER_ADD_TRIGGER = 2;
	public static final int VER_ADD_COLUMNS_TO_DATA_TABLE = 3;
	
	public static final int CUR_DATABASE_VERSION = VER_ADD_COLUMNS_TO_DATA_TABLE;
	public static final String DATA_JOIN_LABELS = 
			Data.TABLE_NAME + " LEFT OUTER JOIN " 
			+ Labels.TABLE_NAME + " ON("
			+ Labels.TABLE_NAME + "." + Labels._ID + "="
			+ Data.TABLE_NAME + "." + Data.LABEL_ID + ")";
	
	private static final String PATH_DATA = "data";
	private static final String PATH_LABELS = "labels";
	
	interface LabelsColumns{
		String NAME = "name";
	}
	interface DataColumns{
		String WEIGHT = "weight";
		String REPEATS = "repeats";
		String RELAX_TIME = "relax_time";
		String DATE = "date";
		String COUNT_APPROACH = "count_approach";
		String COUNT_TRAINING = "count_training";
	}
	
	public static final class Labels implements BaseColumns, LabelsColumns{
		private Labels(){}
		public static final String TABLE_NAME = "labels";
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_LABELS).build();
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tuoved.labels";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tuoved.labels";
		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
		public static Uri buildLabelsUriWithId(long id){
			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}
	
	public static final class Data implements BaseColumns, DataColumns{
		private Data(){}
		public static final String TABLE_NAME = "data";
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_DATA).build();
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tuoved.data";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tuoved.data";
		public static final String LABEL_ID = "label_id";
		public static final String DEFAULT_SORT_ORDER = "date ASC";
		public static Uri buildDataUriWithId( long id ) {
			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}	
	
}
