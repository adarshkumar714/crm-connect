package com.crmconnect.connection;
import org.springframework.stereotype.Component;
@Component public class ConnectionMapper { public ConnectionDto toDto(Connection c){ConnectionDto d=new ConnectionDto();d.setId(c.getId());d.setTenantId(c.getTenantId());d.setFirstName(c.getFirstName());d.setLastName(c.getLastName());d.setEmail(c.getEmail());d.setPhone(c.getPhone());d.setCompanyName(c.getCompanyName());d.setSource(c.getSource());d.setStatus(c.getStatus());d.setAssignedToUserId(c.getAssignedToUserId());d.setCreatedAt(c.getCreatedAt());d.setUpdatedAt(c.getUpdatedAt());return d;} }
