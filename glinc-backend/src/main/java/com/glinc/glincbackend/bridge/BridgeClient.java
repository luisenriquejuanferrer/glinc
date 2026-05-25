package com.glinc.glincbackend.bridge;

import com.glinc.glincbackend.bridge.dto.CreateSessionRequest;
import com.glinc.glincbackend.bridge.dto.PatientsResponse;
import com.glinc.glincbackend.bridge.dto.SessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class BridgeClient {

    private static final Logger log = LoggerFactory.getLogger(BridgeClient.class);

    private final RestClient restClient;

    public BridgeClient(RestClient bridgeRestClient) {
        this.restClient = bridgeRestClient;
    }

    public SessionResponse createSession(String email, String password) {
        String requestId = UUID.randomUUID().toString();

        try {
            CreateSessionRequest peticion = new CreateSessionRequest(email, password);

            SessionResponse respuesta = restClient.post()
                    .uri("/sessions")
                    .header("x-request-id", requestId)
                    .body(peticion)
                    .retrieve()
                    .body(SessionResponse.class);

            if (respuesta == null || respuesta.getSessionId() == null) {
                throw new BridgeException(
                        "El bridge devolvio una respuesta vacia (x-request-id=" + requestId + ")");
            }

            int numPacientes = 0;
            if (respuesta.getPatients() != null) {
                numPacientes = respuesta.getPatients().size();
            }
            log.info("Sesion bridge creada: sessionId={}, pacientes={}, x-request-id={}",
                    respuesta.getSessionId(), numPacientes, requestId);

            return respuesta;

        } catch (RestClientResponseException e) {
            throw new BridgeException("El bridge devolvio " + e.getStatusCode()
                    + " creando sesion (x-request-id=" + requestId + "): "
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new BridgeException(
                    "No se pudo contactar con el bridge (x-request-id=" + requestId + ")", e);
        }
    }

    // 404 se trata como exito: la sesion ya no existia en el bridge, el logout puede continuar.
    public void revokeSession(String sessionId) {
        String requestId = UUID.randomUUID().toString();

        try {
            restClient.delete()
                    .uri("/sessions/{id}", sessionId)
                    .header("x-request-id", requestId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Sesion bridge cerrada: sessionId={}, x-request-id={}",
                    sessionId, requestId);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                log.info("Sesion bridge ya no existe (404): sessionId={}, x-request-id={}",
                        sessionId, requestId);
                return;
            }
            throw new BridgeException("El bridge devolvio " + e.getStatusCode()
                    + " cerrando sesion (x-request-id=" + requestId + "): "
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new BridgeException(
                    "No se pudo contactar con el bridge para cerrar sesion (x-request-id="
                            + requestId + ")", e);
        }
    }

    public PatientsResponse fetchPatients(String sessionId) {
        String requestId = UUID.randomUUID().toString();

        try {
            PatientsResponse respuesta = restClient.get()
                    .uri("/sessions/{id}/patients", sessionId)
                    .header("x-request-id", requestId)
                    .retrieve()
                    .body(PatientsResponse.class);

            if (respuesta == null) {
                throw new BridgeException(
                        "El bridge devolvio una respuesta vacia pidiendo pacientes "
                                + "(x-request-id=" + requestId + ")");
            }
            return respuesta;

        } catch (RestClientResponseException e) {
            throw new BridgeException("El bridge devolvio " + e.getStatusCode()
                    + " pidiendo pacientes (x-request-id=" + requestId + "): "
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new BridgeException(
                    "No se pudo contactar con el bridge pidiendo pacientes "
                            + "(x-request-id=" + requestId + ")", e);
        }
    }
}
