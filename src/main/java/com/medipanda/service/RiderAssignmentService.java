package com.medipanda.service;

import com.medipanda.model.*;
import com.medipanda.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.*;

@Service
public class RiderAssignmentService {
    private final RiderRepository riders; private final OrderRepository orders; private final PharmacyRepository pharmacies; private final NotificationService notifications;
    public RiderAssignmentService(RiderRepository riders, OrderRepository orders, PharmacyRepository pharmacies, NotificationService notifications){this.riders=riders;this.orders=orders;this.pharmacies=pharmacies;this.notifications=notifications;}

    public Optional<Rider> assignNearest(Order order){
        Set<String> rejected=order.getRejectedRiderIds()==null?Set.of():new HashSet<>(order.getRejectedRiderIds());
        List<Rider> available=riders.findByStatus("AVAILABLE").stream().filter(r->!rejected.contains(r.getId())).filter(r->r.getLatitude()!=0&&r.getLongitude()!=0).toList();
        if(available.isEmpty()){order.setRiderId(null);order.setRiderRequestAt(null);order.setStatus("WAITING_FOR_RIDER");orders.save(order);return Optional.empty();}
        Pharmacy p=pharmacies.findById(order.getPharmacyId()).orElse(null);
        double targetLat=p!=null&&p.getLatitude()!=0?p.getLatitude():order.getCustomerLatitude();
        double targetLng=p!=null&&p.getLongitude()!=0?p.getLongitude():order.getCustomerLongitude();
        Rider r=available.stream().min(Comparator.comparingDouble(x->distance(x.getLatitude(),x.getLongitude(),targetLat,targetLng))).orElseThrow();
        r.setStatus("BUSY");riders.save(r);order.setRiderId(r.getId());order.setRiderRequestAt(new Date());order.setStatus("RIDER_REQUESTED");orders.save(order); if(r.getUserId()!=null) notifications.send(r.getUserId(),"New delivery request","You have a new delivery request #"+order.getId().substring(Math.max(0,order.getId().length()-6))+".","DELIVERY",order.getId()); return Optional.of(r);
    }
    public void release(String id){riders.findById(id).ifPresent(r->{r.setStatus("AVAILABLE");riders.save(r);});}
    public void rejectAndReassign(Order order,String riderId){
        if(order.getRejectedRiderIds()==null)order.setRejectedRiderIds(new ArrayList<>());
        if(!order.getRejectedRiderIds().contains(riderId))order.getRejectedRiderIds().add(riderId);
        release(riderId);order.setRiderId(null);order.setRiderRequestAt(null);order.setStatus("READY_FOR_PICKUP");orders.save(order);assignNearest(order);
    }
    @Scheduled(fixedDelay=10000)
    public void timeoutRequests(){
        Date cutoff=new Date(System.currentTimeMillis()-60000);
        orders.findAll().stream().filter(o->"RIDER_REQUESTED".equals(o.getStatus())&&o.getRiderRequestAt()!=null&&o.getRiderRequestAt().before(cutoff)).forEach(o->{
            String current=o.getRiderId();
            if(current!=null){
                if(o.getRejectedRiderIds()==null)o.setRejectedRiderIds(new ArrayList<>());
                if(!o.getRejectedRiderIds().contains(current))o.getRejectedRiderIds().add(current);
                release(current);
            }
            o.setRiderId(null);o.setRiderRequestAt(null);o.setStatus("READY_FOR_PICKUP");orders.save(o);assignNearest(o);
        });
        // If every rider was offline/busy when the pharmacy marked an order ready, retry automatically.
        orders.findAll().stream().filter(o->"WAITING_FOR_RIDER".equals(o.getStatus())).forEach(this::assignNearest);
    }
    private double distance(double a,double b,double c,double d){double x=(a-c)*111,y=(b-d)*102;return Math.sqrt(x*x+y*y);}
}
