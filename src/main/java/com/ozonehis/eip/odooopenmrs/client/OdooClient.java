package com.ozonehis.eip.odooopenmrs.client;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OdooClient {

    @Value("${odoo.baseUrl}")
    private String url;

    @Value("${odoo.database}")
    private String database;

    @Value("${odoo.username}")
    private String username;

    @Value("${odoo.password}")
    private String password;

    private Integer uid;

    private XmlRpcClient client;

    public OdooClient() {}

    public void init() throws IOException {
        client = new XmlRpcClient() {
            {
                setConfig(new XmlRpcClientConfigImpl() {

                    {
                        setServerURL(new URL(String.format("%s/xmlrpc/2/object", getUrl())));
                    }
                });
            }
        };
    }

    public void authenticate() throws MalformedURLException {
        XmlRpcClientConfigImpl common_config = new XmlRpcClientConfigImpl();
        common_config.setServerURL(new URL(String.format("%s/xmlrpc/2/common", getUrl())));

        try {
            uid = (Integer) client.execute(
                    common_config, "authenticate", asList(getDatabase(), getUsername(), getPassword(), emptyMap()));
        } catch (XmlRpcException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot authenticate to Odoo server");
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public String getUid() {
        if (this.uid == null) {
            return "";
        }
        return String.valueOf(this.uid);
    }

    public Object execute(String method, String model, List dataParams, HashMap requestParams)
            throws XmlRpcException, MalformedURLException {
        authenticate();
        List<Object> params;

        if (requestParams == null) {
            params = Collections.singletonList(dataParams);
            return client.execute("execute_kw", asList(getDatabase(), uid, getPassword(), model, method, params));
        } else {
            params = asList(dataParams);
            return client.execute(
                    "execute_kw", asList(getDatabase(), uid, getPassword(), model, method, params, requestParams));
        }
    }

    public ArrayList<String> getDomainFields(String model) throws XmlRpcException {

        Map<String, Map<String, Object>> fieldsResult = (Map<String, Map<String, Object>>) client.execute(
                "execute_kw",
                asList(getDatabase(), uid, getPassword(), model, "fields_get", emptyList(), new HashMap() {
                    {
                        put("attributes", asList("string"));
                    }
                }));

        return new ArrayList<>(fieldsResult.keySet());
    }

    /**
     * Searches and returns entities matching the specified criteria in the odoo instance i.e. by using
     * the semantics of the odoo search_read web service method.
     *
     * @param model    the name of the odoo model to search
     * @param criteria the search criteria to apply e.g. ["name", "=", "test"], ["id", ">", "2"]
     * @param fields   optional list of the model fields to include for each returned item's payload
     * @return an array of matching results
     * @throws XmlRpcException
     * @throws MalformedURLException
     */
    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields)
            throws XmlRpcException, MalformedURLException {

        authenticateIfNecessary();

        List<Object> params = new ArrayList();
        params.add(getDatabase());
        params.add(uid);
        params.add(getPassword());
        params.add(model);
        params.add("search_read");
        params.add(singletonList(singletonList(criteria)));
        if (fields != null) {
            params.add(singletonMap("fields", fields));
        }

        // TODO Add an API for the criteria argument instead of using a list
        return (Object[]) client.execute("execute_kw", params);
    }

    /**
     * Searches and returns ids for entities matching the specified criteria in the odoo instance i.e.
     * by using the semantics of the odoo search web service method.
     *
     * @param model    the name of the odoo model to search
     * @param criteria the search criteria to apply e.g. ["name", "=", "test"], ["id", ">", "2"]
     * @return an array of matching entity ids
     * @throws XmlRpcException
     * @throws MalformedURLException
     */
    public Object[] search(String model, List<Object> criteria) throws XmlRpcException, MalformedURLException {

        authenticateIfNecessary();

        // TODO Add an API for the criteria argument instead of using a list
        return (Object[]) client.execute(
                "execute_kw",
                asList(getDatabase(), uid, getPassword(), model, "search", singletonList(singletonList(criteria))));
    }

    private void authenticateIfNecessary() throws MalformedURLException {
        if (uid == null) {
            authenticate();
        }
    }
}
