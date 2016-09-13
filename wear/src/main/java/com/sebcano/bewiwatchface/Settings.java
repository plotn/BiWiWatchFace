package com.sebcano.bewiwatchface;

import com.sebcano.bewiwatchface.apiwrap.OptionsStorage;
import com.sebcano.bewiwatchface.common.model.TemperatureUnit;

public class Settings {

    private OptionsStorage mStore;

    public Settings( OptionsStorage store ) {
        mStore = store;
    }

    public TemperatureUnit getTemperatureUnit() {
        String unit = mStore.getString( R.string.temperature_key, R.string.temperature_celsius_value );
        if (unit.equals( "F" )) {
            return TemperatureUnit.FAHRENHEIT;
        } else {
            return TemperatureUnit.CELSIUS;
        }
    }


}
