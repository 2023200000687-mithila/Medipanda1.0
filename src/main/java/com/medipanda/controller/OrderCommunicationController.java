package com.medipanda.controller;
import com.medipanda.model.*; import com.medipanda.repository.*; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.security.Principal; import java.util.*;
@RestController @RequestMapping("/api/orders")
public class OrderCommunicationController {
 private final OrderRepository orders; private final UserRepository users; private final com.medipanda.repository.PharmacyRepository pharmacies; private final com.medipanda.repository.RiderRepository riders;
 public OrderCommunicationController(OrderRepository o,UserRepository u,com.medipanda.repository.PharmacyRepository p,com.medipanda.repository.RiderRepository r){orders=o;users=u;pharmacies=p;riders=r;}
 @GetMapping("/{id}/messages") public List<OrderMessage> messages(@PathVariable String id,Principal p){ Order o=authorized(id,p); return o.getMessages()==null?List.of():o.getMessages(); }
 @PostMapping("/{id}/messages") public List<OrderMessage> send(@PathVariable String id,@RequestBody Map<String,String> body,Principal p){
   Order o=authorized(id,p); String text=body.getOrDefault("text","").trim(); if(text.isEmpty()||text.length()>500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Message is empty or too long");
   User u=users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow(); if(o.getMessages()==null)o.setMessages(new ArrayList<>()); o.getMessages().add(OrderMessage.builder().senderRole(u.getRole()).senderName(u.getName()).senderEmail(u.getEmail()!=null?u.getEmail():u.getPhone()).text(text).sentAt(new Date()).build()); orders.save(o); return o.getMessages();
 }
 private Order authorized(String id,Principal p){ Order o=orders.findById(id).orElseThrow(); User u=users.findByEmailIgnoreCaseOrPhone(p.getName(),p.getName()).orElseThrow(); boolean ok=p.getName().equalsIgnoreCase(o.getCustomerEmail())||Set.of("ADMIN").contains(u.getRole());
   if("PHARMACY".equals(u.getRole())) ok=ownsPharmacy(o,u); if("RIDER".equals(u.getRole())) ok=uOwnsRider(o,u); if(!ok)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not part of this order"); return o; }
 private boolean ownsPharmacy(Order o,User u){ return pharmacies.findById(o.getPharmacyId()).map(x->u.getId().equals(x.getOwnerUserId())).orElse(false); }
 private boolean uOwnsRider(Order o,User u){
   if(o.getRiderId()==null) return false;
   Rider r=riders.findById(o.getRiderId()).orElse(null);
   if(r==null) return false;
   // Support older seeded rider documents that were created before userId was stored.
   if(u.getId().equals(r.getUserId())) return true;
   if(r.getUserId()==null && r.getName()!=null && u.getName()!=null && r.getName().equalsIgnoreCase(u.getName())){
      r.setUserId(u.getId());
      riders.save(r);
      return true;
   }
   return false;
 }
}
