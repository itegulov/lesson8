package ru.ifmo.md.lesson8;

public enum WeatherInfo {
    CLEAR("Clear"), SNOW("Snow");

    private String main;
    private String iconName;

    WeatherInfo(String main) {
        this.main = main;
        iconName = main.toLowerCase();
    }

    public static WeatherInfo getWeatherInfo(String s) {
        for (WeatherInfo weatherInfo : WeatherInfo.values()) {
            if (weatherInfo.getMain().equals(s)) {
                return weatherInfo;
            }
        }
        return null;
    }

    public String getMain() {
        return main;
    }
}
