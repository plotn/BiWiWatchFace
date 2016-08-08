package sca.biwiwatchface.data;

public class Weather {
    public static String conditionIdToUnicode( int conditionId ) {
        if (conditionId >= 200 && conditionId <= 232) {
            return "\u26a1"; //⛈ return R.drawable.ic_storm;
        } else if (conditionId >= 300 && conditionId <= 321) {
            return WideUnicode.toString(0x1F326);
            //return "\u2602"; //☂ return R.drawable.ic_light_rain;
        } else if (conditionId >= 500 && conditionId <= 504) {
            return WideUnicode.toString(0x1F327);
            //return "\u2614"; //☔ return R.drawable.ic_rain;
        } else if (conditionId == 511) {
            return "\u2745"; //❅ return R.drawable.ic_snow;
        } else if (conditionId >= 520 && conditionId <= 531) {
            return WideUnicode.toString(0x1F327);
            //return "\u2614"; //☔ return R.drawable.ic_rain;
        } else if (conditionId >= 600 && conditionId <= 622) {
            return "\u2745"; //❅ return R.drawable.ic_snow;
        } else if (conditionId >= 701 && conditionId <= 761) {
            return WideUnicode.toString(0x1F32B);
            //return "\u26c6"; //⛆ return R.drawable.ic_fog;
        } else if (conditionId == 762 || conditionId == 781) {
            return "\u26a1"; //⛈ return R.drawable.ic_storm;
        } else if (conditionId == 800) {
            return "\u2600"; //☀ return R.drawable.ic_clear;
        } else if (conditionId == 801) {
            return "\u26c5"; //⛅ return R.drawable.ic_light_clouds;
        } else if (conditionId >= 802 && conditionId <= 804) {
            return "\u2601"; //☁ return R.drawable.ic_cloudy;
        }
        return "?";
    }

    private static String getAllConditions() {
        String res =
                conditionIdToUnicode(200) +
                        conditionIdToUnicode(300) +
                        conditionIdToUnicode(500) +
                        conditionIdToUnicode(511) +
                        conditionIdToUnicode(520) +
                        conditionIdToUnicode(600) +
                        conditionIdToUnicode(701) +
                        conditionIdToUnicode(762) +
                        conditionIdToUnicode(800) +
                        conditionIdToUnicode(801) +
                        conditionIdToUnicode(802);
        return res;
    }
}
