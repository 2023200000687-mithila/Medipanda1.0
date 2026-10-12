package com.medipanda.controller;

import com.medipanda.model.*;
import com.medipanda.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/location")
public class LocationController {
    private final UserRepository users; private final RiderRepository riders; private final OrderRepository orders; private final PharmacyRepository pharmacies; private final com.medipanda.service.RoutingService routing;
    public LocationController(UserRepository u,RiderRepository r,OrderRepository o,PharmacyRepository p,com.medipanda.service.RoutingService rs){users=u;riders=r;orders=o;pharmacies=p;routing=rs;}

    @PatchMapping("/rider") public Rider rider(@RequestBody Map<String,Object> body,Principal principal){
        User u=findUser(principal); Rider r=ownRider(u); double lat=number(body.get("latitude"),r.getLatitude()),lng=number(body.get("longitude"),r.getLongitude()); validate(lat,lng);
        r.setLatitude(lat);r.setLongitude(lng); if(body.get("address") instanceof String a&&!a.isBlank())r.setLocationAddress(a.trim());r.setLocationUpdatedAt(new Date());return riders.save(r);
    }
    @PatchMapping("/pharmacy") public Pharmacy pharmacy(@RequestBody Map<String,Object> body,Principal principal){
        User u=findUser(principal); Pharmacy p=pharmacies.findByOwnerUserId(u.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No pharmacy linked to this account"));
        double lat=number(body.get("latitude"),p.getLatitude()),lng=number(body.get("longitude"),p.getLongitude());validate(lat,lng);p.setLatitude(lat);p.setLongitude(lng);
        if(body.get("address") instanceof String a&&!a.isBlank())p.setLocationAddress(a.trim());return pharmacies.save(p);
    }
    @GetMapping("/geocode") public Map<String,Object> geocode(@RequestParam String q){
        try{ java.net.URI uri=java.net.URI.create("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q="+java.net.URLEncoder.encode(q,java.nio.charset.StandardCharsets.UTF_8));
            var client=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
            var req=java.net.http.HttpRequest.newBuilder(uri).timeout(java.time.Duration.ofSeconds(8)).header("User-Agent","MediPanda/1.0").header("Accept-Language","en").GET().build();
            var res=client.send(req,java.net.http.HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()!=200)throw new Exception();
            var arr=new com.fasterxml.jackson.databind.ObjectMapper().readTree(res.body()); if(!arr.isArray()||arr.isEmpty())throw new Exception(); var x=arr.get(0);
            return Map.of("latitude",x.get("lat").asDouble(),"longitude",x.get("lon").asDouble(),"display",x.get("display_name").asText());
        }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Address could not be located. Please enter a clearer street, area and city.");}
    }

    @PatchMapping("/customer/order/{id}") public Order customer(@PathVariable String id,@RequestBody Map<String,Object> body,Principal principal){
        Order o=orders.findById(id).orElseThrow(); if(!principal.getName().equalsIgnoreCase(o.getCustomerEmail()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Forbidden");
        double lat=number(body.get("latitude"),o.getCustomerLatitude()),lng=number(body.get("longitude"),o.getCustomerLongitude());if(lat!=0||lng!=0)validate(lat,lng);o.setCustomerLatitude(lat);o.setCustomerLongitude(lng);if(body.get("address") instanceof String a&&!a.isBlank())o.setDeliveryAddress(a.trim());return orders.save(o);
    }
    @GetMapping("/order/{id}") public Map<String,Object> orderLocation(@PathVariable String id,Principal principal){
        Order o=orders.findById(id).orElseThrow();User u=findUser(principal);boolean allowed=principal.getName().equalsIgnoreCase(o.getCustomerEmail())||"ADMIN".equals(u.getRole());
        if("RIDER".equals(u.getRole()))allowed=ownsRiderForUser(o,u);if("PHARMACY".equals(u.getRole()))allowed=pharmacies.findById(o.getPharmacyId()).map(ph->u.getId().equals(ph.getOwnerUserId())).orElse(false);if(!allowed)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not part of this order");
        Map<String,Object> out=new HashMap<>();out.put("customerLatitude",o.getCustomerLatitude());out.put("customerLongitude",o.getCustomerLongitude());out.put("deliveryAddress",o.getDeliveryAddress());out.put("status",o.getStatus());out.put("distanceKm",o.getDistanceKm());out.put("estimatedDeliveryMinutes",o.getEstimatedDeliveryMinutes());out.put("rider",o.getRiderId()==null?null:riders.findById(o.getRiderId()).orElse(null));out.put("pharmacy",pharmacies.findById(o.getPharmacyId()).orElse(null));return out;
    }
    /** Road route between two points (OSM/OSRM). */
    @GetMapping("/route") public Map<String,Object> route(@RequestParam double fromLat,@RequestParam double fromLng,@RequestParam double toLat,@RequestParam double toLng){
        validate(fromLat,fromLng);validate(toLat,toLng);return routing.route(new double[]{fromLat,fromLng},new double[]{toLat,toLng});
    }
    /** Route for an order: rider (if assigned) -> pharmacy (until picked up) -> customer. Same access rules as /order/{id}. */
    @GetMapping("/order/{id}/route") public Map<String,Object> orderRoute(@PathVariable String id,Principal principal){
        Map<String,Object> loc=orderLocation(id,principal);Order o=orders.findById(id).orElseThrow();
        List<double[]> pts=new ArrayList<>();Pharmacy ph=pharmacies.findById(o.getPharmacyId()).orElse(null);Rider r=o.getRiderId()==null?null:riders.findById(o.getRiderId()).orElse(null);
        boolean pickedUp=Set.of("PICKED_UP","ON_THE_WAY","DELIVERED").contains(o.getStatus());
        if(r!=null&&(r.getLatitude()!=0||r.getLongitude()!=0))pts.add(new double[]{r.getLatitude(),r.getLongitude()});
        if(!pickedUp&&ph!=null&&(ph.getLatitude()!=0||ph.getLongitude()!=0))pts.add(new double[]{ph.getLatitude(),ph.getLongitude()});
        if(o.getCustomerLatitude()!=0||o.getCustomerLongitude()!=0)pts.add(new double[]{o.getCustomerLatitude(),o.getCustomerLongitude()});
        if(pts.size()<2)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Route is not available yet");
        Map<String,Object> out=new HashMap<>(routing.route(pts.toArray(new double[0][])));out.put("points",pts);return out;
    }
    private User findUser(Principal p){return users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"User not found"));}
    private Rider ownRider(User u){Rider found=riders.findAll().stream().filter(x->u.getId().equals(x.getUserId())).findFirst().orElse(null);if(found==null)found=riders.findAll().stream().filter(x->x.getName()!=null&&u.getName()!=null&&x.getName().equalsIgnoreCase(u.getName())).findFirst().orElse(null);if(found!=null&&found.getUserId()==null){found.setUserId(u.getId());found=riders.save(found);}if(found==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rider profile not found for this account");return found;}
    private boolean ownsRiderForUser(Order o,User u){if(o.getRiderId()==null)return false;Rider r=riders.findById(o.getRiderId()).orElse(null);if(r==null)return false;if(u.getId().equals(r.getUserId()))return true;if(r.getUserId()==null&&r.getName()!=null&&u.getName()!=null&&r.getName().equalsIgnoreCase(u.getName())){r.setUserId(u.getId());riders.save(r);return true;}return false;}
    private void validate(double lat,double lng){if(lat<-90||lat>90||lng<-180||lng>180||(lat==0&&lng==0))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid GPS coordinates");}
    private double number(Object v,double fallback){if(v instanceof Number n)return n.doubleValue();try{return v==null?fallback:Double.parseDouble(String.valueOf(v));}catch(Exception e){return fallback;}}
}
