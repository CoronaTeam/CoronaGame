package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Objects;

/**
 * Since the double haven't an infinite precision, it is a must to define an approximate version of it.
 * We need to redefine how to hash is computed and how an equality is computed to avoid bugs due to
 * double imprecision.
 */
public final class RoundLocation extends Location {
    public RoundLocation(String provider) {
        super(provider);
    }
    public RoundLocation(Location l){
        super(l);
    }

    @Override
    public int hashCode(){
        return Objects.hash((int)(this.getLatitude()*100000),(int)(this.getLongitude()*100000));
    }
    @Override
    public boolean equals(Object that){
        if(that instanceof Location){
            boolean sameLat =((int)(this.getLatitude()*100000) == (int)(((Location)(that)).getLatitude()*100000));
            boolean sameLong =((int)(this.getLongitude()*100000) == (int)(((Location)(that)).getLongitude()*100000));
            return sameLat && sameLong;
        }
        else{
            return false;
        }
    }
}
