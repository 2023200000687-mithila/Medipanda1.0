package com.medipanda.controller;

import com.medipanda.model.*;
import com.medipanda.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PublicController {
    private final PharmacyRepository pharmacies;
    private final MedicineRepository medicines;
    private final OrderRepository orders;
    private final UserRepository users; private final com.medipanda.service.NotificationService notifications;

    public PublicController(PharmacyRepository p, MedicineRepository m, OrderRepository o, UserRepository u, com.medipanda.service.NotificationService n) {
        pharmacies=p; medicines=m; orders=o; users=u; notifications=n;
    }

    @GetMapping("/pharmacies") public List<Pharmacy> pharmacies(){ return pharmacies.findAll(); }
    @GetMapping("/pharmacies/{id}") public Pharmacy pharmacy(@PathVariable String id){ return pharmacies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pharmacy not found")); }

    @GetMapping("/medicines")
    public List<Medicine> medicines(@RequestParam(required=false) String search, @RequestParam(required=false) String category, @RequestParam(required=false) String pharmacyId){
        if(pharmacyId!=null && !pharmacyId.isBlank()) return medicines.findByPharmacyId(pharmacyId);
        if(search!=null && !search.isBlank()) return medicines.findByNameContainingIgnoreCase(search.trim());
        if(category!=null && !category.isBlank()) return medicines.findByCategoryIgnoreCase(category.trim());
        return medicines.findAll();
    }

    @GetMapping("/medicines/{id}") public Medicine medicine(@PathVariable String id){ return medicines.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Medicine not found")); }

    @PostMapping("/orders")
    public Order create(@RequestBody Order o, Authentication auth){
        if(o.getPharmacyId()==null || o.getDeliveryAddress()==null || o.getDeliveryAddress().isBlank() || o.getItems()==null || o.getItems().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Pharmacy, address and at least one item are required");
        if(!validCoordinate(o.getCustomerLatitude(),o.getCustomerLongitude()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Please use current location or find your manual address on the map before placing the order");
        var user=users.findByEmailIgnoreCaseOrPhone(auth.getName(),auth.getName()).orElseThrow();
        Pharmacy pharmacy=pharmacies.findById(o.getPharmacyId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pharmacy"));
        if(!pharmacy.isOpen()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This pharmacy is currently closed");
        if(o.getTotal()<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid order amount");
        // Orders begin as pending payment; online checkout is initiated through the SSLCommerz endpoint.
        o.setPaymentMethod("COD");
        o.setPaymentStatus("PENDING");
        o.setId(null); o.setCustomerEmail(user.getEmail()!=null?user.getEmail():user.getPhone()); o.setCustomerName(user.getName()); o.setStatus("PENDING"); o.setRiderId(null); o.setRejectedRiderIds(new ArrayList<>()); o.setRiderRequestAt(null); o.setCreatedAt(new Date());
        if(o.getMessages()==null)o.setMessages(new ArrayList<>());
        double distance = distanceKm(pharmacy.getLatitude(), pharmacy.getLongitude(), o.getCustomerLatitude(), o.getCustomerLongitude());
        o.setDistanceKm(distance);
        o.setDeliveryFee(deliveryFee(distance));
        o.setEstimatedDeliveryMinutes(Math.max(25, Math.min(90, 20 + (int)Math.ceil(distance*5))));
        Order saved=orders.save(o);
        pharmacies.findById(saved.getPharmacyId()).ifPresent(ph->{ if(ph.getOwnerUserId()!=null) notifications.send(ph.getOwnerUserId(),"New order received","Order #"+saved.getId().substring(Math.max(0,saved.getId().length()-6))+" is waiting for confirmation.","ORDER",saved.getId()); });
        notifications.notifyCustomer(saved, com.medipanda.service.NotificationService.OrderEvent.PLACED);
        return saved;
    }

    @GetMapping("/customer/orders") public List<Order> customerOrders(Authentication auth){ return orders.findByCustomerEmailOrderByCreatedAtDesc(auth.getName()); }
    private boolean blank(String s){return s==null||s.isBlank();}
    private boolean validCoordinate(double lat,double lng){return lat>=-90&&lat<=90&&lng>=-180&&lng<=180&&(lat!=0||lng!=0);}
    private double distanceKm(double lat1,double lon1,double lat2,double lon2){double R=6371.0,dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));}
    private double deliveryFee(double km){if(km<=2)return 40;if(km<=5)return 50;if(km<=8)return 65;if(km<=12)return 85;if(km<=16)return 110;return 140;}
}
