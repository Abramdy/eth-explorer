package bexchange.eth.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Properties;

public class ApiUtils {

    public static boolean validateParams(Map<String, String> values, String[] fields, HttpServletResponse response) {
        for (int i = 0; i < fields.length; i++) {
            if (!validateParams(values, fields[i], response)) {
                return false;
            }
        }
        return true;

    }

    public static boolean validateParams(Map<String, String> values, String key, HttpServletResponse response) {
        if (!values.containsKey(key)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        return true;
    }

    public static Properties createPropertiesFromRequest(HttpServletRequest request) {

        // New machine log in email
        String ipAddress = request.getRemoteAddr(),
                userAgent = request.getHeader("User-Agent");

        Properties properties = new Properties();

        properties.setProperty("{{user_agent}}", userAgent);
        properties.setProperty("{{client_ip}}", ipAddress);

        return properties;
    }
}
