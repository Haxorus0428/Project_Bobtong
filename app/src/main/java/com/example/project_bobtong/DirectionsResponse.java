package com.example.project_bobtong;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
        public OverviewPolyline overview_polyline;
    }

    public static class Leg {
        public Distance distance;
        public Duration duration;
    }

    public static class Distance {
        public String text;
        public int value; // meters
    }

    public static class Duration {
        public String text;
        public int value; // seconds
    }

    public static class OverviewPolyline {
        public String points;
    }
}
