package com.crmconnect.connection;

import com.crmconnect.tenant.TenantContext;
import com.crmconnect.user.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionServiceTest {
    @Mock ConnectionRepository repository;
    @Mock ConnectionMapper mapper;
    @Mock CurrentUser currentUser;
    @InjectMocks ConnectionService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        TenantContext.setTenantId(7L);
        doNothing().when(currentUser).requireManagerAccess();
    }

    @AfterEach
    void clean() { TenantContext.clear(); }

    @Test
    void createAssignsTenant() {
        ConnectionRequest request = new ConnectionRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any())).thenReturn(new ConnectionDto());

        service.create(request);

        verify(repository).save(argThat(connection -> connection.getTenantId().equals(7L)
                && connection.getFirstName().equals("Ada")));
    }
}
