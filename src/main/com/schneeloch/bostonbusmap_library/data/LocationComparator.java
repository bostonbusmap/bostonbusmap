package com.schneeloch.bostonbusmap_library.data;

import java.util.Comparator;

import com.schneeloch.bostonbusmap_library.math.Geometry;

public class LocationComparator implements Comparator<com.schneeloch.bostonbusmap_library.data.Location> {
	private final double centerLatitudeAsRadians; 
	private final double centerLongitudeAsRadians;


	public LocationComparator(double centerLatitudeAsDegrees, double centerLongitudeAsDegrees)
	{
		centerLatitudeAsRadians = centerLatitudeAsDegrees * Geometry.degreesToRadians;
		centerLongitudeAsRadians = centerLongitudeAsDegrees * Geometry.degreesToRadians;
	}

	public int compare(Location a, Location b)
	{
		double dist = a.distanceFrom(centerLatitudeAsRadians, centerLongitudeAsRadians);
		double otherDist = b.distanceFrom(centerLatitudeAsRadians, centerLongitudeAsRadians);

		int comparison = java.lang.Double.compare(dist, otherDist);
		if (comparison == 0)
		{
			//two different stops or buses at same location
			//if it's equal, the TreeSet may just remove one, assuming equality
			//so we need something else to compare to show that it's not exactly the same stop
			return Integer.valueOf(a.getId()).compareTo(b.getId());
		}
		else
		{
			return comparison;
		}
	}

}