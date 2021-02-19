package cz.devconf2021.lra.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JSONParser {
    private static final Logger log = Logger.getLogger(JSONParser.class);

    public static Map<String,String> parseJson(String jsonData) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String,String> jsonMap = objectMapper.readValue(jsonData, HashMap.class);
            if(log.isDebugEnabled())
                log.debugf("The incoming body '%s' was parsed for JSON format '%s'", jsonData, jsonMap);
            return jsonMap;
        } catch (IOException ioe) {
            log.errorf("Cannot parse the provided body '%s' to JSON format", jsonData);
            throw new WebApplicationException(ioe, Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity(String.format("Cannot parse the provided body '%s' to JSON format", jsonData))
                    .type("text/plain").build());
        }
    }
}
