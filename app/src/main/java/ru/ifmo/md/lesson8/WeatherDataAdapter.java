package ru.ifmo.md.lesson8;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WeatherDataAdapter extends RecyclerView.Adapter<WeatherDataAdapter.WeatherDataViewHolder> {

    private WeatherData[] weatherData;

    public WeatherDataAdapter(WeatherData[] weatherData) {
        this.weatherData = weatherData;
    }

    @Override
    public WeatherDataViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Log.d("WeatherDataAdapter", "Create");
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_min_weather, viewGroup, false);
        return new WeatherDataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WeatherDataViewHolder weatherDataViewHolder, int i) {
        //weatherDataViewHolder.view.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        Log.d("WeatherDataAdapter", "GetItemCount: " + weatherData.length);
        return weatherData.length;
    }

    public class WeatherDataViewHolder extends RecyclerView.ViewHolder {
        public View view;

        public WeatherDataViewHolder(View itemView) {
            super(itemView);
            this.view = itemView;
        }
    }
}
