package com.medipanda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Road routing on OpenStreetMap data via an OSRM server (public demo by default). */
@Service
public class RoutingService {
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public RoutingService(@Value("${app.routing.osrm-base-url:https://router.project-osrm.org}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    /** Points are {lat, lng}. Returns distanceKm, durationMin, geometry (GeoJSON LineString) and source. */
    public Map<String, Object> route(double[]... points) {
        if (points.length < 2) throw new IllegalArgumentException("At least two points are required");
        try {
            StringBuilder coords = new StringBuilder();
            for (double[] p : points) {
                if (coords.length() > 0) coords.append(';');
                coords.append(String.format(Locale.US, "%.6f,%.6f", p[1], p[0])); // OSRM wants lng,lat
            }
            URI uri = URI.create(baseUrl + "/route/v1/driving/" + coords + "?overview=full&geometries=geojson");
            HttpResponse<String> res = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "MediPanda/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new IllegalStateException("OSRM HTTP " + res.statusCode());
            JsonNode root = mapper.readTree(res.body());
            if (!"Ok".equals(root.path("code").asText()) || root.path("routes").isEmpty())
                throw new IllegalStateException("No route");
            JsonNode r = root.path("routes").get(0);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("distanceKm", Math.round(r.path("distance").asDouble() / 10.0) / 100.0);
            out.put("durationMin", (int) Math.ceil(r.path("duration").asDouble() / 60.0));
            out.put("geometry", mapper.convertValue(r.path("geometry"), Map.class));
            out.put("source", "osrm");
            return out;
        } catch (Exception e) {
            return fallback(points);
        }
    }

    /** Straight-line fallback so tracking never breaks if the routing server is down. */
    private Map<String, Object> fallback(double[][] pts) {
        double km = 0;
        var line = new java.util.ArrayList<double[]>();
        for (int i = 0; i < pts.length; i++) {
            line.add(new double[]{pts[i][1], pts[i][0]});
            if (i > 0) km += haversine(pts[i - 1][0], pts[i - 1][1], pts[i][0], pts[i][1]);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("distanceKm", Math.round(km * 100.0) / 100.0);
        out.put("durationMin", (int) Math.ceil(km / 20.0 * 60.0));
        out.put("geometry", Map.of("type", "LineString", "coordinates", line));
        out.put("source", "straight-line");
        return out;
    }

    private static double haversine(double a, double b, double c, double d) {
        double R = 6371.0, dLat = Math.toRadians(c - a), dLon = Math.toRadians(d - b);
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(a)) * Math.cos(Math.toRadians(c)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }
}
