//package ch.epfl.sdp.contamination;
//
//import android.location.Location;
//
//import java.util.Objects;
//
///**
// * Since the double haven't an infinite precision, it is a must to define an approximate version of it.
// * We need to redefine how to hash is computed and how an equality is computed to avoid bugs due to
// * double imprecision.
// */
//public final class RoundLocation extends Location {
//    public RoundLocation(String provider) {
//        super(provider);
//    }
//    public RoundLocation(Location l){
//        super(l);
//    }
//    private int roundMax(double d){
//        return (int)(d*100000);
//    }
//
//    @Override
//    public int hashCode(){
//        return Objects.hash(roundMax(super.getLatitude()),roundMax(super.getLongitude()));
//    }
//    @Override
//    public double getLatitude(){
//        return ((double)(roundMax(super.getLatitude())))/100000;
//    }
//    @Override
//    public double getLongitude(){
//        return ((double)(roundMax(super.getLongitude())))/100000;
//    }
//    @Override
//    public boolean equals(Object that){
//        if(that instanceof Location){
//            boolean sameLat =(roundMax(this.getLatitude()) == roundMax(((Location)(that)).getLatitude()));
//            boolean sameLong =(roundMax(this.getLongitude()) == roundMax(((Location)(that)).getLongitude()));
//            return sameLat && sameLong;
//        }
//        else{
//            return false;
//        }
//    }
//}
