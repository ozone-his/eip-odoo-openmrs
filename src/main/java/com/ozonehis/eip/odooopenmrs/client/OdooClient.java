package com.ozonehis.eip.odooopenmrs.client;

import static java.util.Arrays.asList;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
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

    private static final String SERVER_OBJECT_URL = "%s/xmlrpc/2/object";

    private static final String SERVER_COMMON_URL = "%s/xmlrpc/2/common";

    public OdooClient() {}

    public void init() throws IOException {
        client = new XmlRpcClient() {
            {
                setConfig(new XmlRpcClientConfigImpl() {

                    {
                        setEnabledForExtensions(true);
                        setServerURL(new URL(String.format(SERVER_OBJECT_URL, getUrl())));
                    }
                });
            }
        };
    }

    public void authenticate() throws MalformedURLException {
        // TODO: Where should we place the init logic
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        XmlRpcClientConfigImpl common_config = new XmlRpcClientConfigImpl();
        common_config.setServerURL(new URL(String.format(SERVER_COMMON_URL, getUrl())));

        try {
            uid = (Integer) client.execute(
                    common_config, "authenticate", asList(getDatabase(), getUsername(), getPassword(), emptyMap()));
        } catch (XmlRpcException e) {
            log.error("Cannot authenticate to Odoo server error: {}", e.getMessage());
            throw new RuntimeException("Cannot authenticate to Odoo server");
        }
    }

    public Object create(String method, String model, List dataParams, HashMap requestParams)
            throws XmlRpcException, MalformedURLException {
        authenticateIfNecessary();
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

    public Object write(String model, List<Object> dataParams) throws XmlRpcException, MalformedURLException {
        authenticateIfNecessary();
        return client.execute("execute_kw", asList(getDatabase(), uid, getPassword(), model, "write", dataParams));
    }

    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields)
            throws XmlRpcException, MalformedURLException {

        authenticateIfNecessary();

        List<Object> params = new ArrayList<>();
        params.add(getDatabase());
        params.add(uid);
        params.add(getPassword());
        params.add(model);
        params.add("search_read");
        params.add(singletonList(criteria));
        if (fields != null) {
            params.add(singletonMap("fields", fields));
        }

        return (Object[]) client.execute("execute_kw", params);
    }

    public Object[] search(String model, List<Object> criteria) throws XmlRpcException, MalformedURLException {

        authenticateIfNecessary();

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
