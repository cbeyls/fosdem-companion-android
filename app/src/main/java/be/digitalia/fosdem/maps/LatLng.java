package be.digitalia.fosdem.maps;

/**
 * Created on 6/02/15 for fosdem-companion-android
 *
 * @author bmo
 * @version 1
 */
public class LatLng {
    public double latitude;
    public double longitude;

    public LatLng() {

    }

    public LatLng(double latitude, double longitude) {
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        return  o instanceof LatLng                      &&
                ((LatLng) o).longitude == this.longitude &&
                ((LatLng) o).latitude  == this.latitude;
    }
}
