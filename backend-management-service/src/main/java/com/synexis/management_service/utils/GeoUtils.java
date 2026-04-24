package com.synexis.management_service.utils;

import org.locationtech.jts.geom.*;

public class GeoUtils {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public static Point createPoint(double lat, double lng) {
        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326); // Important: Set the SRID to 4326 for WGS 84
        return point;
    }
}