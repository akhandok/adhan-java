definition(
    name: "Adhan Groovy port",
    author: "Azfar Khandoker",
    description: "",
    iconUrl: "",
    iconX2Url: "",
    namespace: "com.azfarandnusrat.adhanPlayer"
)

preferences {
    page(name: "mainPage") {
        section {
            paragraph "Refreshing... ${initialize()}"
        }
    }
}

def installed() { initialize() }
def updated() { initialize() }
def initialize() {
    log.debug CalculationMethod_MOON_SIGHTING_COMMITTEE().getParameters()
}

// ==========================================================================================================================================
// Hubitat Groovy port of https://github.com/batoulapps/adhan-java/tree/eefc4ed1b910ec144dff247f45b2b23a16c7d0c2
// ==========================================================================================================================================

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

// ==========================================================================================================================================
// Java Enums
// ==========================================================================================================================================

def Enum_ctor(String enumName) {
    def instance = [
        name: enumName
    ];
    instance.equals = { name -> return name == instance.name };
    return instance;
}

// ==========================================================================================================================================
// Coordinates
// ==========================================================================================================================================

def Coordinates_ctor(double latitude, double longitude) {
    return [
        latitude: latitude,
        longitude: longitude
    ];
}

// ==========================================================================================================================================
// DateComponents
// ==========================================================================================================================================

def DateComponents_ctor(int year, int month, int day) {
    return [
        year: year,
        month: month,
        day: day
    ];
}

def DateComponents_from(Date date) {
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setTime(date);
    return DateComponents_ctor(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
}

def DateComponents_fromUTC(Date date) {
    Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.setTime(date);
    return DateComponents_ctor(calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
}

// ==========================================================================================================================================
// PrayerAdjustments
// ==========================================================================================================================================

def PrayerAdjustments_ctor() {
    return [
        fajr: 0,
        sunrise: 0,
        dhuhr: 0,
        asr: 0,
        maghrib: 0,
        isha: 0
    ];
}

def PrayerAdjustments_ctor(int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha) {
    def instance = PrayerAdjustments_ctor()
    instance.fajr = fajr;
    instance.sunrise = sunrise;
    instance.dhuhr = dhuhr;
    instance.asr = asr;
    instance.maghrib = maghrib;
    instance.isha = isha;
    return instance;
}

// ==========================================================================================================================================
// NightPortions
// ==========================================================================================================================================

def NightPortions_ctor(double fajr, double isha) {
    return [
        fajr: fajr,
        isha: isha
    ];
}

// ==========================================================================================================================================
// HighLatitudeRule
// ==========================================================================================================================================

def HighLatitudeRule_MIDDLE_OF_THE_NIGHT() {
    return Enum_ctor('HighLatitudeRule.MIDDLE_OF_THE_NIGHT');
}

def HighLatitudeRule_SEVENTH_OF_THE_NIGHT() {
    return Enum_ctor('HighLatitudeRule.SEVENTH_OF_THE_NIGHT');
}

def HighLatitudeRule_TWILIGHT_ANGLE() {
    return Enum_ctor('HighLatitudeRule.TWILIGHT_ANGLE');
}

// ==========================================================================================================================================
// ShadowLength 
// ==========================================================================================================================================

def ShadowLength_enum(String name, double shadowLength) {
    def instance = Enum_ctor(name);
    instance.shadowLength = shadowLength;
    instance.getShadowLength = { return instance.shadowLength; }
    return instance;
}

def ShadowLength_SINGLE() {
    return ShadowLength_enum('ShadowLength.SINGLE', 1.0);
}

def ShadowLength_DOUBLE() {
    return ShadowLength_enum('ShadowLength.DOUBLE', 2.0);
}

// ==========================================================================================================================================
// Madhab  
// ==========================================================================================================================================

def Madhab_enum(String name, Map shadowLength) {
    def instance = Enum_ctor(name);
    instance.getShadowLength = { return shadowLength };
    return instance;
}

def Madhab_SHAFI() {
    return Madhab_enum('Madhab.SHAFI', ShadowLength_SINGLE())
}

def Madhab_HANAFI() {
    return Madhab_enum('Madhab.HANAFI', ShadowLength_DOUBLE())
}

// ==========================================================================================================================================
// CalculationParameters
// ==========================================================================================================================================

def CalculationParameters_ctor() {
    def instance = [
        method: CalculationMethod_OTHER(),
        fajrAngle: 0,
        ishaAngle: 0,
        ishaInterval: 0,
        madhab: Madhab_SHAFI(),
        highLatitudeRule: HighLatitudeRule_MIDDLE_OF_THE_NIGHT(),
        adjustments: [:],
        methodAdjustments: [:]
    ]
    instance.withMethodAdjustments = { adjustments ->
        instance.methodAdjustments = adjustments;
        return instance;
    }
    instance.nightPortions = {
        switch (instance.highLatitudeRule.name) {
            case HighLatitudeRule_MIDDLE_OF_THE_NIGHT().name:
                return NightPortions_ctor(1.0 / 2.0, 1.0 / 2.0);
            case HighLatitudeRule_SEVENTH_OF_THE_NIGHT().name:
                return NightPortions_ctor(1.0 / 7.0, 1.0 / 7.0);
            case HighLatitudeRule_TWILIGHT_ANGLE().name:
                return NightPortions_ctor(instance.fajrAngle / 60.0, instance.ishaAngle / 60.0);
            default:
                throw new IllegalArgumentException("Invalid high latitude rule");
        }
    }
    return instance;
}

def CalculationParameters_ctor(double fajrAngle, double ishaAngle) {
    def instance = CalculationParameters_ctor()
    instance.fajrAngle = fajrAngle;
    instance.ishaAngle = ishaAngle;
    return instance;
}

def CalculationParameters_ctor(double fajrAngle, int ishaInterval) {
    def instance = CalculationParameters_ctor(fajrAngle, 0.0);
    instance.ishaInterval = ishaInterval;
    return instance;
}

def CalculationParameters_ctor(double fajrAngle, double ishaAngle, Map method) {
    def instance = CalculationParameters_ctor(fajrAngle, ishaAngle);
    instance.method = method;
    return instance;
}

def CalculationParameters_ctor(double fajrAngle, int ishaInterval, Map method) {
    def instance = CalculationParameters_ctor(fajrAngle, ishaInterval);
    instance.method = method;
    return instance;
}

// ==========================================================================================================================================
// CalculationMethod 
// ==========================================================================================================================================

def CalculationMethod_enum(String name, double fajrAngle, double ishaAngle) {
    def instance = Enum_ctor(name);
    instance.getParameters = {
        return CalculationParameters_ctor(fajrAngle, ishaAngle, instance);
    }
    return instance;
}

def CalculationMethod_enum(String name, double fajrAngle, int ishaInterval) {
    def instance = Enum_ctor(name);
    instance.getParameters = {
        return CalculationParameters_ctor(fajrAngle, ishaInterval, instance);
    }
    return instance;
}

def CalculationMethod_enum(String name, double fajrAngle, double ishaAngle, Map adjustments) {
    def instance = Enum_ctor(name);
    instance.getParameters = {
        return CalculationParameters_ctor(fajrAngle, ishaAngle, instance).withMethodAdjustments(adjustments);
    }
    return instance;
}

def CalculationMethod_MUSLIM_WORLD_LEAGUE() {
    def adjustments = PrayerAdjustments_ctor(0, 0, 1, 0, 0, 0);
    return CalculationMethod_enum('CalculationMethod.MUSLIM_WORLD_LEAGUE', 18.0, 17.0, adjustments);
}

def CalculationMethod_EGYPTIAN() {
    def adjustments = PrayerAdjustments_ctor(0, 0, 1, 0, 0, 0);
    return CalculationMethod_enum('CalculationMethod.EGYPTIAN', 19.5, 17.5, adjustments);
}

def CalculationMethod_KARACHI() {
    def adjustments = PrayerAdjustments_ctor(0, 0, 1, 0, 0, 0);
    return CalculationMethod_enum('CalculationMethod.KARACHI', 18.0, 18.0, adjustments);
}

def CalculationMethod_UMM_AL_QURA() {
    return CalculationMethod_enum('CalculationMethod.UMM_AL_QURA', 18.5, 90);
}

def CalculationMethod_DUBAI() {
    def adjustments = PrayerAdjustments_ctor(0, -3, 3, 3, 3, 0);
    return CalculationMethod_enum('CalculationMethod.DUBAI', 18.2, 18.2, adjustments);
}

def CalculationMethod_MOON_SIGHTING_COMMITTEE() {
    def adjustments = PrayerAdjustments_ctor(0, 0, 5, 0, 3, 0);
    return CalculationMethod_enum('CalculationMethod.MOON_SIGHTING_COMMITTEE', 18.0, 18.0, adjustments);
}

def CalculationMethod_NORTH_AMERICA() {
    def adjustments = PrayerAdjustments_ctor(0, 0, 1, 0, 0, 0);
    return CalculationMethod_enum('CalculationMethod.NORTH_AMERICA', 15.0, 15.0, adjustments);
}

def CalculationMethod_KUWAIT() {
    return CalculationMethod_enum('CalculationMethod.KUWAIT', 18.0, 17.5);
}

def CalculationMethod_QATAR() {
    return CalculationMethod_enum('CalculationMethod.QATAR', 18.0, 90);
}

def CalculationMethod_SINGAPORE() {
    def adjustments = PrayerAdjustments_ctor(0, 0, 1, 0, 0, 0);
    return CalculationMethod_enum('CalculationMethod.SINGAPORE', 20.0, 18.0, adjustments);
}

def CalculationMethod_OTHER() {
    return CalculationMethod_enum('CalculationMethod.OTHER', 0.0, 0.0);
}
































