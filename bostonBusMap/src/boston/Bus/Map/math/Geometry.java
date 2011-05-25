package boston.Bus.Map.math;

import android.util.FloatMath;

/**
 * All non-specific math stuff used to calculate angles and distances should go in here
 * @author schneg
 *
 */
public class Geometry {
	
	/**
	 * Used in calculating the distance between coordinates
	 */
	private static final double radiusOfEarthInKilo = 6371.2;
	private static final double kilometersPerMile = 1.609344;
	
	
	private static final double radiusOfEarthInMiles = radiusOfEarthInKilo / kilometersPerMile;

	public static final double degreesToRadians = java.lang.Math.PI / 180.0;

	public static final double radiansToDegrees = 180.0 / java.lang.Math.PI;

	private static final double InvPITimes180 = (1.0 / Math.PI) * 180;
	
	/**
	 * For testing only
	 * 
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return
	 */
	public static float computeCompareDistanceFloat(float lat1, float lon1, float lat2, float lon2)
	{
		//great circle distance
		//double dist = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
		float dist = (((1.0f - FloatMath.cos(lat1 - lat2)) * 0.5f) + FloatMath.cos(lat1) * FloatMath.cos(lat2) * 
				((1.0f - FloatMath.cos(lon1 - lon2)) * 0.5f)); 
		return dist * (float)radiusOfEarthInMiles;
	}
	
	/**
	 * Returns a compare distance, or a distance which is distorted for efficiency sake but comparisons are always correct 
	 * 
	 * @param lat1 latitude in radians
	 * @param lat2 longitude in radians
	 * @param lat2 latitude in radians
	 * @param lon2 longitude in radians
	 * @return distance in miles
	 */
	public static float computeCompareDistance(double lat1, double lon1, double lat2, double lon2)
	{
		double dist = (((1.0 - Math.cos(lat1 - lat2)) * 0.5) + Math.cos(lat1) * Math.cos(lat2) * 
				((1.0 - Math.cos(lon1 - lon2)) * 0.5)); 
		return (float) (dist * radiusOfEarthInMiles);
	}

	public static int getDegreesFromSlope(double y, double x)
	{
		double thetaInRadians = Math.atan2(y, x);
		
		int degrees = mathRadiansToDegrees(thetaInRadians);
		return degrees;
	}
	
	/**
	 * Convert from radians starting east and going counterclockwise, to a degree direction starting north and going clockwise
	 * 
	 * @param thetaBackup direction in radians, where east is 0 and going counterclockwise
	 * @return a descriptive String showing the direction (for example: E (90 deg))
	 */
	public static int mathRadiansToDegrees(double thetaAsRadians)
	{
		//NOTE: degrees will be 0 == north, going clockwise
		int degrees = (int)(thetaAsRadians * InvPITimes180);
		if (degrees < 0)
		{
			degrees += 360;
		}
		
		//convert to usual compass orientation
		degrees = -degrees + 90;
		if (degrees < 0)
		{
			degrees += 360;
		}
		
		return degrees;
	}


	
}
