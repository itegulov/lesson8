package ru.ifmo.md.lesson8;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WeatherLoaderService extends IntentService {
    public static final String API_KEY = "af1fd362b895c36e1a37e182defcf246";
    public static final String SERVICE_NAME = WeatherLoaderService.class.getSimpleName();
    public static final String LOG_TAG = WeatherLoaderService.class.getSimpleName();

    public static final String CITY_NAME = "city_name";
    //Handler codes:
    public static final int UPDATED = 0;
    public static final int ALREADY_UPDATED = 1;
    public static final int UPDATING = 2;
    public static final int ERROR = -1;
    public static final int NUMBER_OF_DAYS = 7;
    public static final long UPDATE_INTERVAL = 0L; //Ten minutes
    private static final List<String> tasks = new ArrayList<>();
    private static Handler handler;

    public WeatherLoaderService() {
        super(SERVICE_NAME);
    }

    public static void setHandler(Handler handler) {
        WeatherLoaderService.handler = handler;
    }

    public static void loadCity(Context context, String name) {
        context.startService(new Intent(context, WeatherLoaderService.class).putExtra(CITY_NAME, name));
    }

    public static boolean isLoading(String cityName) {
        return tasks.contains(cityName);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        String cityName = intent.getStringExtra(CITY_NAME);
        if (!tasks.contains(cityName)) {
            tasks.add(cityName);
            super.onStart(intent, startId);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "" + tasks.size());
        String cityName = intent.getStringExtra(CITY_NAME);
        Cursor cursor = getApplicationContext().getContentResolver().
                query(WeatherContentProvider.CITY_CONTENT_URI, null,
                        WeatherDatabaseHelper.CITY_NAME + " = '" + cityName + "'", null, null);
        cursor.moveToNext();
        if (cursor.isAfterLast()) {
            //Delete first element
            tasks.remove(0);
            return;
        }

        City city = WeatherDatabaseHelper.CityCursor.getCity(cursor);
        if (isAlreadyUpdated(city)) {
            if (handler != null) {
                //It's already updated, so we don't need to do anything
                handler.obtainMessage(ALREADY_UPDATED).sendToTarget();
            }
            //Delete first element
            tasks.remove(0);
            return;
        }

        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(LOG_TAG, "Nyaaaa~~~~");
                }
            });
            //We began updating
            Log.d(LOG_TAG, "write UPDATING to handler");
            handler.obtainMessage(UPDATING).sendToTarget();
        }

        //TODO: implement

        WeatherData[] weatherData = loadWeatherInCity(city);
        Log.d(LOG_TAG,
                Integer.toString(weatherData[0].getTemperature()) + " " + weatherData.length);
        Log.d(LOG_TAG, "Begin updating");
        if (update(weatherData, city)) {
            Log.d(LOG_TAG, "Update success");
            if (handler != null) {
                //We successfully updated the database
                Log.d(LOG_TAG, handler.toString());
                handler.obtainMessage(UPDATED).sendToTarget();
                Log.d(LOG_TAG, "Write UPDATED to handler");
            }
        } else {
            Log.d(LOG_TAG, "Update error");
            if (handler != null) {
                //Error has occurred
                handler.obtainMessage(ERROR).sendToTarget();
            }
        }
        tasks.remove(0); //Proceeded one task
        Log.d(LOG_TAG, "Finished proceeding a task");
    }

    private WeatherData[] loadWeatherInCity(City city) {
        return FetchWeatherTask.getWeatherInCity(city, NUMBER_OF_DAYS, API_KEY);
    }

    private boolean update(WeatherData[] weatherData, City city) {
        if (weatherData == null || weatherData.length == 0) {
            return false;
        }

        int count = getContentResolver().delete(WeatherContentProvider.WEATHER_CONTENT_URI,
                WeatherDatabaseHelper.WEATHER_CITY_ID + " = " + city.getId(), null);
        Log.d(LOG_TAG, "Deleted " + city.getId() + " " + city.getName() + " " + count);

        for (WeatherData data : weatherData) {
            Log.d(LOG_TAG, "Data: " + data.toString());
            Uri uri = getContentResolver().insert(WeatherContentProvider.WEATHER_CONTENT_URI, data.getContentValues());
            Log.d(LOG_TAG, "Inserted URI : " + uri.toString());
        }
        Log.d(LOG_TAG, "Finished inserting");
        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherDatabaseHelper.CITY_UPDATE_DATE, new Date().getTime()); //We updated the city
        getContentResolver().update(WeatherContentProvider.CITY_CONTENT_URI, contentValues,
                WeatherDatabaseHelper.CITY_ID + " = " + city.getId(), null);
        Log.d(LOG_TAG, "Finished update");
        return true;
    }

    private boolean isAlreadyUpdated(City city) {
        Date currentDate = new Date();
        return currentDate.getTime() <= (city.getUpdateDate() + UPDATE_INTERVAL);
    }
}
