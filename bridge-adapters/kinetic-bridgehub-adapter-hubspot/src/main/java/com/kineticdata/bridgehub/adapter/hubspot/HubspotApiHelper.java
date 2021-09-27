package com.kineticdata.bridgehub.adapter.hubspot;

import com.kineticdata.bridgehub.adapter.BridgeError;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a Rest service helper.
 */
public class HubspotApiHelper {
    private static final Logger LOGGER = 
        LoggerFactory.getLogger(HubspotApiHelper.class);
    
    private final String baseUrl;
    private final String apiKey;
    
    public HubspotApiHelper(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }
    
    public JSONObject executeRequest (String path, JSONObject body) throws BridgeError{
        String url = baseUrl + path + "?hapikey=" + apiKey;
        
        HttpPost post = new HttpPost(url);
        StringEntity requestEntity = null;
        
        try {
            requestEntity = new StringEntity(body.toJSONString());
        } catch(UnsupportedEncodingException e) {
            throw new BridgeError("An exception occured during encoding json string", e);
        }
        post.setEntity(requestEntity);
        post.setHeader("Content-Type", "application/json");
            
        return executeRequest(post);
    }
    
    public JSONObject executeRequest (String path) throws BridgeError {
        String url = baseUrl + path;
        
        // Append the api key to the end of the url for authntication
        url = url.contains("?") 
            ? url + "&hapikey=" + apiKey
            : url + "?hapikey=" + apiKey;
        
        HttpGet get = new HttpGet(url);

        get.setHeader("Content-Type", "application/json");
        get.setHeader("Accept", "application/json");

        return executeRequest (get);
    }
        
    public JSONObject executeRequest (HttpRequestBase http) throws BridgeError{
        JSONObject output;      
        // System time used to measure the request/response time
        long start = System.currentTimeMillis();
        
        try (
            CloseableHttpClient client = HttpClients.createDefault()
        ) {
            HttpResponse response;
            
            response = client.execute(http);
            LOGGER.debug("Recieved response from \"{}\" in {}ms.",
                http.getURI(),
                System.currentTimeMillis()-start);

            int responseCode = response.getStatusLine().getStatusCode();
            LOGGER.trace("Request response code: " + responseCode);
            
            HttpEntity entity = response.getEntity();
            
            // Confirm that response is a JSON object
            output = parseResponse(EntityUtils.toString(entity));
            
            // Handle all other failed repsonses
            if (responseCode >= 400) {
                handleFailedReqeust(responseCode);
            }
        }
        catch (IOException e) {
            throw new BridgeError(
                "Unable to make a connection to the Harvest service server.", e);
        }
        
        return output;
    }
    
    private void handleFailedReqeust (int responseCode) throws BridgeError {
        switch (responseCode) {
            case 400:
                throw new BridgeError("400: Bad Reqeust");
            case 401:
                throw new BridgeError("401: Unauthorized");
            case 404:
                throw new BridgeError("404: Page not found");
            case 405:
                throw new BridgeError("405: Method Not Allowed");
            case 500:
                throw new BridgeError("500 Internal Server Error");
            default:
                throw new BridgeError("Unexpected response from server");
        }
    }
        
    private JSONObject parseResponse(String output) throws BridgeError{
        
        JSONObject responseObj = new JSONObject();
        try {
            responseObj = (JSONObject)JSONValue.parseWithException(output);
            // A message in the response means that the request failded with a 400
            if(responseObj.containsKey("message")) {
                throw new BridgeError(String.format("The server responded with: "
                    + "\"%s\"", responseObj.get("message")));
            }
        } catch (ParseException e){
            // Assume all 200 responses will be JSON format.
            LOGGER.error("There was a parse exception with the response", e);
        } catch (BridgeError e) {
            throw e;
        } catch (Exception e) {
            throw new BridgeError("An unexpected error has occured ", e);
        }
        
        return responseObj;
    }
}
