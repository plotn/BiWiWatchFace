package sca.biwiwatchface.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class WeatherContract {

    public static final String CONTENT_AUTHORITY = "sca.biwiwatchface.provider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_WEATHER = "weather";

    public static final class WeatherEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

        public static final String TABLE_NAME = "weather";

        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WEATHER_ID = "weather_id";
        public static final String COLUMN_TEMP = "temp";
        public static final String COLUMN_MIN_TEMP = "min_temp";
        public static final String COLUMN_MAX_TEMP = "max_temp";
        public static final String COLUMN_CITY_NAME = "city_name";

        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }

}
