package com.kineticdata.bridgehub.adapter.hubspot;

import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HubspotTest extends BridgeAdapterTestBase {

    @Override
    public Class getAdapterClass() {
        return HubspotAdapter.class;
    }

    @Override
    public String getConfigFilePath() {
        return "src/test/resources/bridge-config.yml";
    }

    @Test
    @Override
    public void test_emptyRetrieve() throws Exception {
        // stub
        assertTrue(true);
    }
    
    @Test
    @Override
    public void test_emptySearch() throws Exception {
        // stub
        assertTrue(true);
    }
    
    @Test
    @Override
    public void test_emptyCount() throws Exception {
        // stub
        assertTrue(true);
    }
    
    @Test
    public void test_count() throws Exception{
        BridgeError error = null;

        BridgeRequest request = new BridgeRequest();

        List<String> fields = Arrays.asList("id");
        request.setFields(fields);

        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("");
        
        Count count = null;
        try {
            count = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(count.getValue() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/crm/v3/objects/companies?accessor=results");
        
        Count adhocCount = null;
        try {
            adhocCount = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(Objects.equals(adhocCount.getValue(), count.getValue()));
    }

    @Test
    public void test_search() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.name");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("");
                
        Map parameters = new HashMap();
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/crm/v3/objects/companies?accessor=results");
        
        RecordList adhocList = null;
        try {
            adhocList = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(list.getRecords().size() == adhocList.getRecords().size());
        
        fields.clear();
        fields.add("id");
        fields.add("$.properties.lastname");
        
        request.setStructure("Contacts");
        request.setQuery("");
        
        list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }
    
    @Test
    public void test_search_associations() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.name");
        fields.add("$.associations.tickets.results");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("associations=tickets");
                
        Map parameters = new HashMap();
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }
    
    @Test
    public void test_search_error() throws Exception {
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        // This id doesn't exist.  Will result in an http 404.
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("Id", "1234");
        }};

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setQuery("id=<%=parameter[\"Id\"]%>");
        request.setFields(fields);
        request.setParameters(parameters);

        try {
            getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNotNull(error);
    }
    
    @Test
    public void test_limit() throws Exception {
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<>();
        fields.add("id");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Companies");
        request.setQuery("");
        request.setFields(fields);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() == 100);

        // Create the Bridge Request
        fields = new ArrayList<String>();
        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("body={\"query\": \"<%=parameter[\"Query\"]%>\"}");
                
        Map parameters = new HashMap();
        parameters.put("Query", "a");
        request.setParameters(parameters);

        list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() == 100);
    }
    
    @Test
    public void test_query() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.name");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("body={\"query\": \"<%=parameter[\"Query\"]%>\"}");
                
        Map parameters = new HashMap();
        parameters.put("Query", "BANKING");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    @Test
    public void test_filter() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.name");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("body= {"+
                            "\"filterGroups\": [{" +
                                "\"filters\": [{" + 
                                    "\"value\": \"<%=parameter[\"Query\"]%>\"," +
                                    "\"propertyName\": \"industry\"," +
                                    "\"operator\": \"NEQ\"" +
                                "}]" +
                            "}]," +
                            "\"limit\": 100" +
                        "}");
        
        Map parameters = new HashMap();
        parameters.put("Query", "BANKING");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }
    
    @Test
    public void test_sort() throws Exception {
        BridgeError error = null;
        
        Map<String,String> sortOrder = new HashMap<String,String>() {{
            put("order", "<%=field[\"name\"]%>:ASC");
        }};

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.name");
        
        Map parameters = new HashMap() {{
            put("Query", "BANKING");
        }};

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Companies");
        request.setFields(fields);
        request.setQuery("body= {"+
                            "\"query\": \"a\"," +
                        "}");
        request.setMetadata(sortOrder);
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }
    
    // This tests using metadata and query to do paging
    @Test
    public void test_paging() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.firstname");
        fields.add("$.properties.lastname");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);        
        request.setQuery("limit=1");
        
        Map parameters = new HashMap();
        request.setParameters(parameters);
        
        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        
        String nextPage = list.getMetadata().get("next_page");
        
        // Test passing the next page through the metadata
        Map<String, String> metadata = new HashMap<String, String>() {{
            put("page", nextPage);
        }};
        request.setMetadata(metadata);
        
        RecordList pagedList = null;
        try {
            pagedList = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(pagedList.getRecords().size() > 0);
        assertTrue(list.getRecords().get(0).getRecord().get("id") != 
            pagedList.getRecords().get(0).getRecord().get("id"));
        
        // Test adding the next page token to the query 
        request.setQuery("limit=1&after=" + nextPage);
        
        pagedList = null;
        try {
            pagedList = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(pagedList.getRecords().size() > 0);
        assertTrue(list.getRecords().get(0).getRecord().get("id") != 
            pagedList.getRecords().get(0).getRecord().get("id"));
    }

    @Test
    public void test_retrieve() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        fields.add("$.properties.firstname");
        fields.add("$.properties.lastname");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contacts");
        request.setFields(fields);        
        request.setQuery("");
        
        Map parameters = new HashMap();
        request.setParameters(parameters);
        
        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        
        String id = (String)list.getRecords().get(0).getRecord().get("id");
        
        request.setQuery("id=<%=parameter[\"Contact Id\"]%>");
        parameters.put("Contact Id", id);
        request.setParameters(parameters);

        Record record = null;
        try {
            record = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(record.getRecord().containsKey("id"));
        
        request.setStructure("Adhoc");
        request.setQuery("/crm/v3/objects/contacts/<%=parameter[\"Contact Id\"]%>"
            + "?accessor=results");
        
        Record adhocRecord = null;
        try {
            adhocRecord = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertEquals(record.getRecord(),adhocRecord.getRecord());
    }
}
