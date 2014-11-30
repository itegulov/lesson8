package ru.ifmo.md.lesson8;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class WeatherFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = WeatherFragment.class.getSimpleName();
    public static final String CITY_ID_EXTRA = "city_id";
    public static final String CITY_NAME_EXTRA = "city_name";

    private int cityId;
    private String cityName;

    private WeatherDataAdapter adapter;
    private RecyclerView weatherRecycleView;
    private View mainView;
    private int selectedDay = -1;
    private Handler handler;
    private boolean userRequestUpdate = false;

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "onCreateLoader() Fragment");
        CursorLoader cursorLoader = new CursorLoader(getActivity(), WeatherContentProvider.WEATHER_CONTENT_URI, null,
                WeatherDatabaseHelper.WEATHER_CITY_ID + " = " + cityId, null,
                WeatherDatabaseHelper.WEATHER_DATE + " asc");
        Log.d(LOG_TAG, "Finish onCreateLoader() Fragment");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(LOG_TAG, "onLoadFinished() Fragment");
        if (adapter == null) {
            adapter = new WeatherDataAdapter(getActivity());
            adapter.setOnItemClickListener(new WeatherDataAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View v, int pos) {
                    adapter.setCurrentItem(pos);
                    setDescriptionWeather(pos);
                }
            });
            weatherRecycleView.setAdapter(adapter);
        }
        if (cursor.isAfterLast()) {
            return;
        }
        adapter.clear();
        WeatherDatabaseHelper.WeatherDataCursor wc = new WeatherDatabaseHelper.WeatherDataCursor(cursor);
        while (cursor.moveToNext()) {
            adapter.add(wc.getWeatherData());
        }
        adapter.notifyDataSetChanged();
        setDescriptionWeather(0);
        adapter.setCurrentItem(0);
    }

    public void setDescriptionWeather(int id) {
        WeatherData weatherData = adapter.getItem(id);
        selectedDay = id;
        ((TextView) mainView.findViewById(R.id.temperatureTextView)).setText(
                weatherData.getTemperatureMin() + "°C/" + weatherData.getTemperatureMax() + "°C");
        ((TextView) mainView.findViewById(R.id.pressureTextView)).setText(
                weatherData.getPressure() + " mb");
        ((TextView) mainView.findViewById(R.id.windTextView)).setText(
                weatherData.getWindSpeed() + " m/s");
        if (weatherData.getHumidity() != 0) {
            ((TextView) mainView.findViewById(R.id.humidityTextView)).setText(
                    weatherData.getHumidity() + "%");
        } else {
            ((TextView) mainView.findViewById(R.id.humidityTextView)).setText("-");
        }
        try {
            AssetManager manager = getActivity().getAssets();
            Log.d("WeatherIcons", weatherData.getWeatherInfo().getIconName());
            Bitmap bitmap = BitmapFactory.decodeStream(manager.open(weatherData.getWeatherInfo().getIconName()));
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            Configuration config = getActivity().getResources().getConfiguration();
            mainView.findViewById(R.id.iconWeatherBig).setBackground(new BitmapDrawable(new Resources(manager, metrics, config), bitmap));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(LOG_TAG, "onLoaderReset() Fragment");
        adapter = null;
        weatherRecycleView.setAdapter(null);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate() Fragment");
        super.onCreate(savedInstanceState);
        cityId = getArguments().getInt(CITY_ID_EXTRA);
        cityName = getArguments().getString(CITY_NAME_EXTRA);
        setRetainInstance(true);
        if (isOnline()) {
            startLoading(false);
        } else {
            Toast.makeText(getActivity(), "Check your internet connection", Toast.LENGTH_SHORT).show();
        }

        Log.d(LOG_TAG, "Create handler");
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d("WeatherHandler", "Handling a message: " + msg.toString());
                if (msg.what == WeatherLoaderService.UPDATED) {
                    getActivity().getLoaderManager().restartLoader(75436789, null, WeatherFragment.this);
                    stopLoading();
                } else if (msg.what == WeatherLoaderService.UPDATING) {
                    getActivity().getActionBar().setSubtitle("Updating");
                } else if (msg.what == WeatherLoaderService.ALREADY_UPDATED) {
                    stopLoading();
                    if (userRequestUpdate) {
                        Toast.makeText(getActivity(), "Weather has already been updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    public void startLoading(boolean userRequestUpdate) {
        Log.d(LOG_TAG, "Start loading");
        this.userRequestUpdate = userRequestUpdate;
        WeatherLoaderService.loadCity(getActivity(), cityName);
    }

    public void stopLoading() {
        Log.d(LOG_TAG, "Stop loading");
        getActivity().getActionBar().setSubtitle(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        if (item.getItemId() == R.id.menu_item_refresh) {
            if (!isOnline())
                Toast.makeText(getActivity(), "Check your internet connection", Toast.LENGTH_SHORT).show();
            else
                startLoading(true);
        }
        return true;
        */
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_weather, container, false);
        mainView = view;
        setHasOptionsMenu(true);
        weatherRecycleView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        weatherRecycleView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        weatherRecycleView.setHasFixedSize(true);
        if (adapter != null) {
            weatherRecycleView.setAdapter(adapter);
        } else {
            Log.d(LOG_TAG, "Restart loader");
            getLoaderManager().restartLoader(24562, null, this);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        WeatherLoaderService.setHandler(handler);
        Log.i(LOG_TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();

        WeatherLoaderService.setHandler(null);
        getActivity().getActionBar().setSubtitle(null);
    }
}
