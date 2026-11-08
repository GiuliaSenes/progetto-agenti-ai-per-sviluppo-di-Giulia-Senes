package it.orizon.co2.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.orizon.co2.ecofreight.EcoFreightClient;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Function calling: definizione del tool e sua esecuzione verso EcoFreight. */
@Component
public class Co2Tool {

    public static final String NAME = "calculate_co2";

    /** Veicolo predefinito per ogni modalità (l'utente non tecnico non deve sceglierlo). */
    private static final Map<String, String> DEFAULT_VEHICLE = Map.of(
            "road", "light_commercial_under_7_5t",
            "rail", "diesel_freight",
            "sea", "container_ship_3000_8000_teu",
            "air", "belly_cargo");

    private final EcoFreightClient client;
    private final ObjectMapper mapper;

    public Co2Tool(EcoFreightClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** Definizione del tool in formato OpenAI "tools". */
    public JsonNode definition() {
        try {
            return mapper.readTree("""
                {"type":"function","function":{
                  "name":"calculate_co2",
                  "description":"Calcola le emissioni di CO2 (kg CO2e) di un viaggio dati mezzo di trasporto, peso trasportato, origine e destinazione.",
                  "parameters":{"type":"object","properties":{
                    "origin":{"type":"string","description":"Luogo di partenza, es. 'Roma, Italia'"},
                    "destination":{"type":"string","description":"Luogo di arrivo, es. 'Tokyo, Giappone'"},
                    "transport_mode":{"type":"string","enum":["road","rail","sea","air"],"description":"road=strada, rail=treno, sea=nave, air=aereo"},
                    "weight_kg":{"type":"number","description":"Peso trasportato in kg (1-100000)"}
                  },"required":["origin","destination","transport_mode","weight_kg"]}}}
                """);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Esegue il tool con gli argomenti JSON decisi dall'LLM; restituisce sempre un JSON (anche in caso di errore). */
    public String execute(String argumentsJson) {
        try {
            JsonNode a = mapper.readTree(argumentsJson);
            String origin = a.path("origin").asText("").trim();
            String destination = a.path("destination").asText("").trim();
            String mode = a.path("transport_mode").asText("").toLowerCase();
            double weight = a.path("weight_kg").asDouble(0);
            if (origin.isEmpty() || destination.isEmpty() || !DEFAULT_VEHICLE.containsKey(mode) || weight <= 0) {
                return error("Parametri non validi: servono origin, destination, transport_mode (road|rail|sea|air) e weight_kg > 0.");
            }
            if (weight < 1 || weight > 100000) {
                return error("Il peso deve essere tra 1 e 100000 kg.");
            }
            ObjectNode result = client.calculate(origin, destination, weight, mode, DEFAULT_VEHICLE.get(mode), "general");
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return error("Argomenti del tool non leggibili: " + e.getMessage());
        }
    }

    private String error(String msg) {
        return mapper.createObjectNode().put("error", msg).toString();
    }
}
