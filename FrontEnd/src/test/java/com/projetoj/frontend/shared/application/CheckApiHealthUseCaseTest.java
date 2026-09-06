package com.projetoj.frontend.shared.application;

import com.projetoj.frontend.shared.http.ApiClient;
import com.projetoj.frontend.shared.http.HealthStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckApiHealthUseCaseTest {

    @Mock
    private ApiClient apiClient;

    @Test
    void shouldReturnHealthIncludingDatabase() {
        when(apiClient.getHealth()).thenReturn(new HealthStatus("UP", "UP", "UP"));

        CheckApiHealthUseCase useCase = new CheckApiHealthUseCase(apiClient);
        HealthStatus health = useCase.execute();

        assertEquals("UP", health.getStatus());
        assertEquals("UP", health.getApi());
        assertEquals("UP", health.getDatabase());
        assertTrue(health.isHealthy());
    }
}
