package it.orizon.co2.ecofreight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.orizon.co2.config.EcoFreightProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/** Chiamata server-side all'API EcoFreight CO2 (POST /calculate). */
@Component
public class EcoFreightClient {

    private final RestClient http;
    private final ObjectMapper mapper;
    private final EcoFreightProperties props;

    public EcoFreightClient(EcoFreightProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.http = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    /** Restituisce un JSON compatto (o un errore descrittivo) da passare all'LLM come risultato del tool. */
    public ObjectNode calculate(String origin, String destination, double weightKg,
                                String mode, String vehicleType, String cargoType) {
        ObjectNode out = mapper.createObjectNode();
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            return out.put("error", "ECOFREIGHT_API_KEY non configurata sul server.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", Map.of("query", origin));
        body.put("destination", Map.of("query", destination));
        body.put("cargo", Map.of("weight", weightKg, "type", cargoType == null ? "general" : cargoType));
        body.put("transport_mode", mode);
        if (vehicleType != null && !vehicleType.isBlank()) body.put("vessel_type", vehicleType);

        try {
            JsonNode r = http.post().uri("/calculate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(JsonNode.class);
            out.put("co2_kg", r.path("emissions").path("wtw").asDouble());
            out.put("co2_kg_direct_ttw", r.path("emissions").path("ttw").asDouble());
            out.put("distance_km", r.path("calculation").path("distance").asDouble());
            out.put("transport_mode", mode);
            out.put("weight_kg", weightKg);
            out.put("origin", origin);
            out.put("destination", destination);
            out.put("calculation_id", r.path("calculation_id").asText());
            return out;
        } catch (HttpStatusCodeException e) {
            return out.put("error", "EcoFreight HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            return out.put("error", "Errore chiamando EcoFreight: " + e.getMessage());
        }
    }
}
