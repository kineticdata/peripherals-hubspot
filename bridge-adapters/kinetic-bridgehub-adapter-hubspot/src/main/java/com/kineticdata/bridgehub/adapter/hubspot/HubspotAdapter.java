package com.kineticdata.bridgehub.adapter.hubspot;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.BridgeUtils;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

public class HubspotAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    public HubspotAdapter () {
        // Parse the query and exchange out any parameters with their parameter 
        // values. ie. change the query username=<%=parameter["Username"]%> to
        // username=test.user where parameter["Username"]=test.user 
        parser = new HubspotQualificationParser();
        
    }
    
    /*----------------------------------------------------------------------------------------------
     * STRUCTURES
     *      AdapterMapping( Structure Name, accessor, Path Function)
     *--------------------------------------------------------------------------------------------*/
    public static Map<String,AdapterMapping> MAPPINGS 
        = new HashMap<String,AdapterMapping>() {{
        put("Companies", new AdapterMapping("Companies", "results",
            HubspotAdapter::pathCompanies));
        put("Contacts", new AdapterMapping("Contacts", "results",
            HubspotAdapter::pathContacts));
        put("Tickets", new AdapterMapping("Tickets", "results",
            HubspotAdapter::pathTickets));
        put("Adhoc", new AdapterMapping("Adhoc", "",
            HubspotAdapter::pathAdhoc));
    }};

    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/

    /** Defines the adapter display name */
    public static final String NAME = "Hubspot Bridge";

    /** Defines the LOGGER */
    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HubspotAdapter.class);

    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties
                    .load(HubspotAdapter.class.getResourceAsStream("/" + HubspotAdapter.class.getName() + ".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            LOGGER.warn("Unable to load " + HubspotAdapter.class.getName() + " version properties.", e);
            VERSION = "Unknown";
        }
    }

    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String PROPERTY_API_KEY = "API Key";
    }
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.PROPERTY_API_KEY).setIsRequired(true));

    // Local variables to store the property values in
    private final HubspotQualificationParser parser;
    private HubspotApiHelper apiHelper;

    private static final String API_PATH = "https://api.hubapi.com";

    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public void initialize() throws BridgeError {
        // Initializing the variables with the property values that were passed
        // when creating the bridge so that they are easier to use
        String apiKey = properties.getValue(Properties.PROPERTY_API_KEY);
        apiHelper = new HubspotApiHelper(API_PATH, apiKey);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void setProperties(Map<String, String> parameters) {
        // This should always be the same unless there are special circumstances
        // for changing it
        properties.setValues(parameters);
    }

    @Override
    public ConfigurablePropertyMap getProperties() {
        // This should always be the same unless there are special circumstances
        // for changing it
        return properties;
    }
    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public Count count(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Counting records");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }

        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        // Convet the query string into a map of query parameters to values.       
        Map<String, String> parameters = getParameters(request.getQuery(), mapping);
        // Replace <%=parameter["..."]%> with value.
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            entry.setValue(parser.parse(entry.getValue(),request.getParameters()));
        }
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));
        
        // Get the number of elements in the returned array
        Long tempCount = (Long)responseObject.get("total_entries");
        Integer count = 0;
        // Single results will not have a total_entries property
        if (tempCount == null) {
            Long singleResult = (Long)responseObject.get("id");
            if (singleResult == null) {
                throw new BridgeError("The Count result was unexpected.  Please"
                        + "check query and rerun.");
            } else {
                // If object has id property assume a single result for found
                count = 1;
            }
        } else {
            count = (int) tempCount.intValue();
        }

        // Create and return a count object that contains the count
        return new Count(count);
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Retrieving Kinetic Request CE Record");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            LOGGER.trace("  Fields: " + request.getFieldString());
        }
        
        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        // Convet the query string into a map of query parameters to values.       
        Map<String, String> parameters = getParameters(request.getQuery(), mapping);
        // Replace <%=parameter["..."]%> with value.
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            entry.setValue(parser.parse(entry.getValue(),request.getParameters()));
        }
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
                
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);

        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));
        
        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        Record record = new Record();
        if (responseArray.size() == 1) {
            // Reassign object to single result 
            JSONObject object = (JSONObject)responseArray.get(0);
                
            List<String> fields = getFields(request.getFields() == null ? 
                new ArrayList() : request.getFields(), object);
            record = buildRecord(fields, object);
        } else if (responseArray.isEmpty()) {
            LOGGER.debug("No results found for query: {}", request.getQuery());
        } else {
            throw new BridgeError ("Retrieve must return a single result."
                + " Multiple results found.");
        }

        // Return the created Record object
        return record;
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Searching Records");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            LOGGER.trace("  Fields: " + request.getFieldString());
        }
        
        // Parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // Get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        // Convet the query string into a map of query parameters to values.       
        Map<String, String> parameters = getParameters(request.getQuery(), mapping);
        // Replace <%=parameter["..."]%> with value.
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            entry.setValue(parser.parse(entry.getValue(),request.getParameters()));
        }
        // Add pagination to parameters.
        addPagination(parameters, request.getMetadata());
  
        // Hubspot searching requires a POST request with json body
        boolean isSearch = false;
        JSONObject body = new JSONObject();
        if (parameters.containsKey("body")) {
            isSearch = true; 
            try {
                body = (JSONObject)JSONValue.parseWithException(parameters.get("body"));
            } catch (ParseException e){
                throw new BridgeError("'body' parameter was not valid JSON", e);
            }
        }
        
        
        if (!isSearch) {
            // Add limit to pramaeters if it does not exist. 100 is hubspot max.
            parameters.putIfAbsent("limit", "100");
        } else {
            // Add limit to body if it does not exist. 100 is hubspot max.
            body.putIfAbsent("limit", "100");
            
            // Get sort order items
            LinkedHashMap<String,String> sortOrderItems = null; 
            if (request.getMetadata("order") != null && !body.containsKey("sorts")) { 
                sortOrderItems = getSortOrderItems(
                    BridgeUtils.parseOrder(request.getMetadata("order")));
                
                if (sortOrderItems.size() == 1) {
                    Entry<String, String> firstItem = 
                        sortOrderItems.entrySet().iterator().next();
                    
                    JSONObject sortsObj = new JSONObject() {{
                        put("propertyName", firstItem.getKey());
                        put("direction", firstItem.getValue().equals("ASC") ? "ASCENDING" : "DESCENDING");
                    }};
                    
                    JSONArray sorts = new JSONArray();
                    sorts.add(sortsObj);
                    // Add sort to body
                    body.put("sorts", sorts);
                } else {
                    throw new BridgeError("HubSpot only supports a sort on one property.");
                }
            }
        }
        
        List<String> fields = request.getFields() == null ? new ArrayList() : 
            request.getFields(); 
        
        // Hubspot specific requirement to add "properties" fields.  This is how
        // custom fields are fetched.
        if (!fields.isEmpty()) {
            parameters = setProperties(fields, parameters);
        }
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = isSearch 
            ? apiHelper.executeRequest(path, body) 
            : apiHelper.executeRequest(getUrl(path, parameterMap));
        
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        // Create a List of records that will be used to make a RecordList object.
        List<Record> recordList = new ArrayList<>();          
        if(responseArray != null && responseArray.isEmpty() != true){
            fields = getFields(fields, (JSONObject)responseArray.get(0));

            // Iterate through the responce objects and make a new Record for each.
            for (Object o : responseArray) {
                JSONObject obj = (JSONObject)o;
                Record record = buildRecord(fields, obj);
                
                // Add the created record to the list of records
                recordList.add(record);
            }
        }

        // Add next page token to metadata.
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("next_page", getNextPage(responseObject));

        // Return the RecordList object
        return new RecordList(fields, recordList, metadata);
    }

    /*--------------------------------------------------------------------------
     * HELPER METHODS
     *------------------------------------------------------------------------*/
    private Map<String, String> setProperties(List<String> fields, 
        Map<String, String> parameters) {
        
        List<String> properties = new ArrayList<>();

        Pattern pDot = Pattern.compile("(?<=properties.).*");
        Pattern pBracket = Pattern.compile("(?<=properties\\[\").*.+?(?=\")");
        
        fields.forEach(field -> {
            Matcher mDot = pDot.matcher(field);
            Matcher mBracket = pBracket.matcher(field);
            
            if( mDot.find()){
               properties.add(mDot.group());
            }
            if( mBracket.find()) {
                properties.add(mBracket.group());
            }
        });
        
        if (!properties.isEmpty()) {
            parameters.put("properties", StringUtils.join(properties, ","));
        }
        
        return parameters;
    }
    
    private String getNextPage (JSONObject responseObject) {
        String nextPage = "";
        
        try {
            nextPage = JsonPath.parse(responseObject).read("$.paging.next.after");
        } catch (Exception e) {
            /* There is not concern with this execption. JsonPath will force an
             * exception if the json object does not have a matching path. To keep 
             * the adapter generic we always check if there is a next page token.  
             */
        }
        
        return nextPage;
    }
    
    protected List<String> getFields(List<String> fields, JSONObject jsonobj) {
        // if no fields were provided then all fields will be returned. 
        if(fields.isEmpty()){
            fields.addAll(jsonobj.keySet());
        }
        
        return fields;
    }
    
    /**
     * Build a Record.  If no fields are provided all fields will be returned.
     * 
     * @param fields
     * @param jsonobj
     * @return Record
     */
    protected Record buildRecord (List<String> fields, JSONObject jsonobj) {
        JSONObject obj = new JSONObject();
        DocumentContext jsonContext = JsonPath.parse(jsonobj); 
        
        fields.stream().forEach(field -> {
            // either use JsonPath or just add the field value.  We're assuming
            // all JsonPath usages will begin with $[ or $.. 
            if (field.startsWith("$.") || field.startsWith("$[")) {
                try {
                    obj.put(field, jsonContext.read(field));
                } catch (JsonPathException e) {
                    // if field is a valid path but object is missing the property
                    // return null for field.  This is consistent with existing 
                    // adapter behavior.
                    if (e.getMessage().startsWith("Missing property")) {
                        obj.put(field, null);
                        LOGGER.debug(String.format("%s was not found, returning"
                            + " null value", field), e);
                    } else {   
                        throw new JsonPathException(String.format("There was an issue"
                            + " reading %s", field), e);
                    }
                }
            } else {
                obj.put(field, jsonobj.get(field));
            }
        });
        
        Record record = new Record(obj, fields);
        return record;
    }
    
        
    protected JSONArray getResponseData(Object responseData) {
        JSONArray responseArray = new JSONArray();
        
        if (responseData instanceof JSONArray) {
            responseArray = (JSONArray)responseData;
        }
        else if (responseData instanceof JSONObject) {
            // It's an object
            responseArray.add((JSONObject)responseData);
        }
        
        return responseArray;
    }
    
    /**
     * Get accessor value. If structure is Adhoc remove accessor from parameters.
     * 
     * @param mapping
     * @param parameters
     * @return 
     */
    private String getAccessor(AdapterMapping mapping, Map<String, String> parameters) {
        String accessor;
        
        if (mapping.getStructure().equals("Adhoc")) {
            accessor = parameters.get("accessor");
            parameters.remove("accessor");
        } else {
            accessor = mapping.getAccessor();
        }
        
        return accessor;
    }
    
    /**
     * This helper is intended to abstract the parser get parameters from the core
     * methods.
     * 
     * @param request
     * @param mapping
     * @return
     * @throws BridgeError
     */
    protected Map<String, String> getParameters(String query, AdapterMapping mapping) throws BridgeError {

        Map<String, String> parameters = new HashMap<>();
        if (mapping.getStructure() == "Adhoc") {
            // Adhoc qualifications are two segments. ie path?queryParameters
            String[] segments = query.split("[?]", 2);

            // getParameters only needs the queryParameters segment
            if (segments.length > 1) {
                parameters = parser.getParameters(segments[1]);
            }
            // Pass the path along to the functional operator
            parameters.put("adapterPath", segments[0]);
        } else {
            parameters = parser.getParameters(query);
        }

        return parameters;
    }

    /**
     * This method checks that the structure on the request matches on in the
     * Mapping internal class. Mappings map directly to the adapters supported
     * Structures.
     * 
     * @param structure
     * @return Mapping
     * @throws BridgeError
     */
    protected AdapterMapping getMapping(String structure) throws BridgeError {
        AdapterMapping mapping = MAPPINGS.get(structure);
        if (mapping == null) {
            throw new BridgeError("Invalid Structure: '" + structure + "' is not a valid structure");
        }
        return mapping;
    }

    protected Map<String, NameValuePair> buildNameValuePairMap(Map<String, String> parameters) {
        Map<String, NameValuePair> parameterMap = new HashMap<>();

        parameters.forEach((key, value) -> {
            parameterMap.put(key, new BasicNameValuePair(key, value));
        });

        return parameterMap;
    }

    private Map<String, String> addPagination(Map<String, String> parameters,
            Map<String, String> metadata) {

        if (metadata != null) {
            if (metadata.containsKey("page")) {
                // Hubspot calls its paging token "after".
                parameters.putIfAbsent("after", metadata.get("page"));
            }
        }

        return parameters;
    }
    
    protected String getUrl (String path,
        Map<String, NameValuePair> parameters) {
        
        String url = parameters.isEmpty() ? path : String.format("%s?%s", path, 
            URLEncodedUtils.format(parameters.values(), Charset.forName("UTF-8")));
        
        return url;
    }
    
    /**
     * Ensure that the sort order list is linked so that order can not be changed.
     * 
     * @param uncastSortOrderItems
     * @return
     * @throws IllegalArgumentException 
     */
    private LinkedHashMap<String, String> 
        getSortOrderItems (Map<String, String> uncastSortOrderItems)
        throws IllegalArgumentException{
        
        /* results of parseOrder does not allow for a structure that 
         * guarantees order.  Casting is required to preserver order.
         */
        if (!(uncastSortOrderItems instanceof LinkedHashMap)) {
            throw new IllegalArgumentException("Sort Order Items was invalid.");
        }
        
        return (LinkedHashMap)uncastSortOrderItems;
    }
    
    /**************************** Path Definitions ****************************/
    protected static String pathCompanies(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {
        
        String path = "/crm/v3/objects/companies";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        if (parameters.containsKey("body")) {
            path = String.format("%s/%s", path, "search");
            parameters.remove("body");
        }
        
        return path;
    }   
    
    protected static String pathContacts(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/crm/v3/objects/contacts";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        if (parameters.containsKey("body")) {
            path = String.format("%s/%s", path, "search");
            parameters.remove("body");
        }
        
        return path;
    }
    
    protected static String pathTickets(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/crm/v3/objects/tickets";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
   
        return path;
    }          
       
    /**
     * Build path for Adhoc structure.
     * 
     * @param structureList
     * @param parameters
     * @return
     * @throws BridgeError 
     */
    protected static String pathAdhoc(List<String> structureList, 
        Map<String, String> parameters) throws BridgeError {
        
        return parameters.get("adapterPath");
    }

    /**
     * Checks if a parameter exists in the parameters Map.
     * 
     * @param param
     * @param parameters
     * @param structureList
     * @throws BridgeError 
     */
    protected static void checkRequiredParamForStruct(String param,
        Map<String, String> parameters, List<String> structureList)
        throws BridgeError{
        
        if (!parameters.containsKey(param)) {
            String structure = String.join(" > ", structureList);
            throw new BridgeError(String.format("The %s structure requires %s"
                + "parameter.", structure, param));
        }
    }
}
