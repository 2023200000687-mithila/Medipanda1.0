package com.medipanda.controller;

import com.medipanda.model.*;
import com.medipanda.repository.*;
import com.medipanda.service.RiderAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

@RestController @RequestMapping("/api/rider")
public class RiderController {
    private final RiderRepository riders; private final OrderRepository orders; private final UserRepository users; private final RiderAssignmentService service; private final com.medipanda.service.NotificationService notifications;
    public RiderController(RiderRepository r,OrderRepository o,UserRepository u,RiderAssignmentService s,com.medipanda.service.NotificationService n){riders=r;orders=o;users=u;service=s;notifications=n;}

    @GetMapping("/me") public Rider me(Principal principal){ return own(principal); }
    @GetMapping("/orders") public List<Order> myOrders(Principal principal){ return orders.findByRiderIdOrderByCreatedAtDesc(own(principal).getId()); }

    @PatchMapping("/orders/{orderId}/accept") public Order accept(@PathVariable String orderId, Principal principal){
        Rider rider=own(principal); Order o=orders.findById(orderId).orElseThrow();
        if(!rider.getId().equals(o.getRiderId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This delivery is not assigned to you");
        if(!"RIDER_REQUESTED".equals(o.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This request is no longer available");
        // Accepting a delivery consumes the request and keeps the rider BUSY.
        rider.setStatus("BUSY");
        riders.save(rider);
        o.setStatus("RIDER_ACCEPTED");
        o.setRiderRequestAt(null);
        Order saved=orders.save(o);
        users.findByEmailIgnoreCaseOrPhone(saved.getCustomerEmail(),saved.getCustomerEmail()).ifPresent(c->notifications.send(c.getId(),"Rider assigned","Your rider has accepted order #"+saved.getId().substring(Math.max(0,saved.getId().length()-6))+".","DELIVERY",saved.getId()));
        return saved;
    }

    @PatchMapping("/orders/{orderId}/reject") public Order reject(@PathVariable String orderId, Principal principal){
        Rider rider=own(principal); Order o=orders.findById(orderId).orElseThrow();
        if(!rider.getId().equals(o.getRiderId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This delivery is not assigned to you");
        service.rejectAndReassign(o,rider.getId()); return orders.findById(orderId).orElse(o);
    }

    @PatchMapping("/orders/{orderId}/status/{status}") public Order orderStatus(@PathVariable String orderId,@PathVariable String status,Principal principal){
        Rider rider=own(principal); Order o=orders.findById(orderId).orElseThrow(); if(!rider.getId().equals(o.getRiderId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Forbidden");
        String s=status.toUpperCase(); if(!Set.of("PICKED_UP","ON_THE_WAY","DELIVERED").contains(s)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid delivery status");
        if("PICKED_UP".equals(s) && !"RIDER_ACCEPTED".equals(o.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Accept the delivery first");
        if("ON_THE_WAY".equals(s) && !"PICKED_UP".equals(o.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Order must be picked up first");
        if("DELIVERED".equals(s) && !"ON_THE_WAY".equals(o.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Order must be on the way first");
        o.setStatus(s); if("DELIVERED".equals(s)){ rider.setStatus("AVAILABLE"); rider.setEarnings(rider.getEarnings()+o.getDeliveryFee()); riders.save(rider); } Order saved=orders.save(o); if("DELIVERED".equals(s)) notifications.notifyCustomer(saved,com.medipanda.service.NotificationService.OrderEvent.DELIVERED); else users.findByEmailIgnoreCaseOrPhone(saved.getCustomerEmail(),saved.getCustomerEmail()).ifPresent(c->notifications.send(c.getId(),"Order update","Your order is now "+s.replace("_"," ")+".","DELIVERY",saved.getId())); return saved;
    }

    @PatchMapping("/status/{status}") public Rider status(@PathVariable String status,Principal principal){ Rider r=own(principal); String s=status.toUpperCase(); if(!Set.of("AVAILABLE","OFFLINE").contains(s)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid rider status"); if("BUSY".equals(r.getStatus())&&"AVAILABLE".equals(s)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Finish the active delivery before becoming available"); r.setStatus(s); return riders.save(r); }
    private Rider own(Principal p){ User u=users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow(); return riders.findAll().stream().filter(r -> (r.getUserId()!=null && r.getUserId().equals(u.getId())) || (r.getUserId()==null && r.getName().equalsIgnoreCase(u.getName()))).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Rider profile not found")); }
}
