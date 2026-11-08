package it.orizon.co2.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.orizon.co2.config.LlmProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Client per API "chat completions" compatibili OpenAI con function calling.
 * Gestisce il ciclo: LLM -> richiesta tool -> esecuzione -> LLM -> risposta finale.
 */
@Component
public class LlmClient {

    private static final int MAX_TOOL_ROUNDS = 4;

    private final RestClient http;
    private final LlmProperties props;
    private final ObjectMapper mapper;
    private final Co2Tool tool;

    public LlmClient(LlmProperties props, ObjectMapper mapper, Co2Tool tool) {
        this.props = props;
        this.mapper = mapper;
        this.tool = tool;
        this.http = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    /** @param messages system + storico + nuovo messaggio utente (formato OpenAI); viene esteso con i turni del tool. */
    public String complete(ArrayNode messages) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new LlmException("LLM_API_KEY non configurata sul server.");
        }
        ArrayNode tools = mapper.createArrayNode().add(tool.definition());

        for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", props.model());
            body.put("temperature", 0.2);
            body.set("messages", messages);
            if (round < MAX_TOOL_ROUNDS) body.set("tools", tools); // all'ultimo giro forza la risposta testuale

            JsonNode resp = post(body);
            JsonNode msg = resp.path("choices").path(0).path("message");
            JsonNode calls = msg.path("tool_calls");

            if (!calls.isArray() || calls.isEmpty()) {
                String text = msg.path("content").asText("");
                return text.isBlank() ? "Non sono riuscito a generare una risposta, puoi riprovare?" : text;
            }

            messages.add(msg); // turno assistant con le tool_calls
            for (JsonNode call : calls) {
                String result = Co2Tool.NAME.equals(call.path("function").path("name").asText())
                        ? tool.execute(call.path("function").path("arguments").asText("{}"))
                        : "{\"error\":\"tool sconosciuto\"}";
                ObjectNode toolMsg = mapper.createObjectNode();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", call.path("id").asText());
                toolMsg.put("content", result);
                messages.add(toolMsg);
            }
        }
        throw new LlmException("Troppi cicli di chiamata al tool.");
    }

    private JsonNode post(ObjectNode body) {
        try {
            return http.post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(JsonNode.class);
        } catch (HttpStatusCodeException e) {
            throw new LlmException("Errore dal servizio LLM (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new LlmException("Servizio LLM non raggiungibile: " + e.getMessage());
        }
    }

    public static class LlmException extends RuntimeException {
        public LlmException(String m) { super(m); }
    }
}
