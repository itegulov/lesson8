package ru.ifmo.md.lesson8;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WeatherLoaderService extends IntentService {
    public static final String API_KEY = "af1fd362b895c36e1a37e182defcf246";
    public static final String SERVICE_NAME = WeatherLoaderService.class.getSimpleName();

    public static final String CITY_NAME = "city_name";
    //Handler codes:
    public static final int UPDATED = 0;
    public static final int ALREADY_UPDATED = 1;
    public static final int UPDATING = 2;
    public static final int ERROR = -1;
    public static final int NUMBER_OF_DAYS = 7;
    public static final long UPDATE_INTERVAL = 10L * 60L; //Ten minutes
    private static final List<String> tasks = new ArrayList<>();
    private Handler handler;

    public WeatherLoaderService() {
        super(SERVICE_NAME);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
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
            //We began updating
            handler.obtainMessage(UPDATING).sendToTarget();
        }

        //TODO: implement

        WeatherData[] weatherData = loadWeatherInCity(city);

        if (update(weatherData, city)) {
            if (handler != null) {
                //We successfully updated the database
                handler.obtainMessage(UPDATED).sendToTarget();
            }
        } else {
            if (handler != null) {
                //Error has occurred
                handler.obtainMessage(ERROR).sendToTarget();
            }
        }

    }

    private WeatherData[] loadWeatherInCity(City city) {
        return FetchWeatherTask.getWeatherInCity(city, NUMBER_OF_DAYS, API_KEY);
    }

    private boolean update(WeatherData[] weatherData, City city) {
        if (weatherData == null || weatherData.length == 0) {
            return false;
        }

        getContentResolver().delete(WeatherContentProvider.WEATHER_CONTENT_URI,
                WeatherDatabaseHelper.WEATHER_CITY_ID + " = " + city.getId(), null);

        for (WeatherData data : weatherData) {
            getContentResolver().insert(WeatherContentProvider.WEATHER_CONTENT_URI, data.getContentValues());
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherDatabaseHelper.CITY_UPDATE_DATE, new Date().getTime()); //We updated the city
        getContentResolver().update(WeatherContentProvider.CITY_CONTENT_URI, contentValues,
                WeatherDatabaseHelper.CITY_ID + " = " + city.getId(), null);
        return true;
    }

    private boolean isAlreadyUpdated(City city) {
        Date currentDate = new Date();
        return currentDate.getTime() <= 1000L * (city.getUpdateDate() + UPDATE_INTERVAL);
    }
}
