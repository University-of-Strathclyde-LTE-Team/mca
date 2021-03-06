/*
 * Copyright (c) 2010, University of Bristol
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the University of Bristol nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ilrt.mca.rest.resource;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sdb.SDBFactory;
import com.hp.hpl.jena.sdb.Store;
import com.hp.hpl.jena.sdb.sql.SDBConnection;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.test.framework.WebAppDescriptor;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ilrt.mca.RdfMediaType;
import org.ilrt.mca.rest.providers.FreemarkerTemplateProvider;
import org.ilrt.mca.rest.providers.JenaModelRdfProvider;
import org.ilrt.mca.rest.resources.AbstractResourceTest;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mike Jones (mike.a.jones@bristol.ac.uk)
 */
public class SparqlEndpointResourceEnabledTest extends AbstractResourceTest {

    public SparqlEndpointResourceEnabledTest() {

        super(new WebAppDescriptor.Builder("org.ilrt.mca.rest")
                .initParam("sparqlEnabled", "true").build());

        List supportedClasses = new ArrayList();
        supportedClasses.add(JenaModelRdfProvider.class);
        supportedClasses.add(FreemarkerTemplateProvider.class);

        this.setSupportedClasses(supportedClasses);

        webResource = resource().path("/sparql"); // one request path for all tests
    }

    @Test
    public void testWithoutQuery() {

        ClientResponse response = getClientResponse(null, RdfMediaType.APPLICATION_RDF_XML);

        assertEquals("Unexpected response code", Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testGibberishQuery() throws UnsupportedEncodingException {

        ClientResponse response = getClientResponse(GIBBERISH_QUERY,
                RdfMediaType.APPLICATION_RDF_XML);

        assertEquals("Unexpected response code", Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testDescribeQueryRdfXml() {

        ClientResponse response = getClientResponse(DESCRIBE_QUERY,
                RdfMediaType.APPLICATION_RDF_XML);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        Model m = response.getEntity(Model.class);

        assertTrue("Expected the model to have triples", m.size() > 0);
    }

    @Test
    public void testDescribeQueryN3() {

        ClientResponse response = getClientResponse(DESCRIBE_QUERY,
                RdfMediaType.TEXT_RDF_N3);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        String s = response.getEntity(String.class);

        assertTrue("Expected to find URI: " + EXPECTED_URI + " in response",
                s.contains(EXPECTED_URI));
    }

    @Test
    public void testConstructQueryRdfXml() {

        ClientResponse response = getClientResponse(CONSTRUCT_QUERY,
                RdfMediaType.APPLICATION_RDF_XML);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        Model m = response.getEntity(Model.class);

        assertTrue("Expected the model to have triples", m.size() > 0);
    }

    @Test
    public void testConstructQueryN3() {

        ClientResponse response = getClientResponse(CONSTRUCT_QUERY,
                RdfMediaType.TEXT_RDF_N3);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        String s = response.getEntity(String.class);

        assertTrue("Expected to find URI: " + EXPECTED_URI + " in response",
                s.contains(EXPECTED_URI));
    }

    @Test
    public void testSelectQueryXml() throws IOException {

        ClientResponse response = getClientResponse(SELECT_QUERY, RdfMediaType.SPARQL_RESULTS_XML);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        String s = response.getEntity(String.class);
        validateXml(s);
    }

    @Test
    public void testSelectQueryJson() throws IOException {

        ClientResponse response = getClientResponse(SELECT_QUERY, RdfMediaType.SPARQL_RESULTS_JSON);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        String s = response.getEntity(String.class);
        validateJson(s);
    }

    @Test
    public void testAskQueryXml() throws IOException {

        ClientResponse response = getClientResponse(ASK_QUERY, RdfMediaType.SPARQL_RESULTS_XML);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        String s = response.getEntity(String.class);
        validateXml(s);
    }

    @Test
    public void testAskQueryJson() throws IOException {

        ClientResponse response = getClientResponse(ASK_QUERY, RdfMediaType.SPARQL_RESULTS_JSON);

        assertEquals("Unexpected response code", Response.Status.OK.getStatusCode(),
                response.getStatus());

        String s = response.getEntity(String.class);
        validateJson(s);
    }

    // ---------- Implement method that is abstract in the super class

    protected void setUpDatabase() {

        // create a connection and store
        SDBConnection conn = createConnection();
        Store store = createStore(conn);

        // format the database
        store.getTableFormatter().format();
        store.getTableFormatter().truncate();

        // add the data
        Dataset dataset = SDBFactory.connectDataset(store);
        Model model = dataset.getDefaultModel();
        model.read(getClass().getResourceAsStream(TEST_REGISTRY), null, "TTL");

        // clean up
        store.close();
        conn.close();
    }

    private ClientResponse getClientResponse(String query, String mediaType) {

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query", query);

        return webResource.queryParams(queryParams).accept(mediaType).get(ClientResponse.class);
    }

    private void validateXml(String result) throws IOException {
        try {
            XMLReader reader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
            reader.parse(new InputSource(new StringReader(result)));
        } catch (SAXException ex) {
            fail("Unable to parse result as xml: " + ex.getMessage());
        }
    }

    private void validateJson(String result) {
        try {
            new JSONObject(result);
        } catch (JSONException ex) {
            fail("Unable to parse result as JSON: " + ex.getMessage());
        }
    }


    //private WebResource webResource = null;

    private final String DESCRIBE_QUERY = "DESCRIBE <mca://registry/>";
    private final String CONSTRUCT_QUERY = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
    private final String SELECT_QUERY = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
    private final String ASK_QUERY = "ASK { ?s ?p ?o }";
    private final String GIBBERISH_QUERY = "ahhkjha hja hjhkj akjh";


    private final String EXPECTED_URI = "mca://registry";
}
