package com.ozonehis.eip.odooopenmrs.client;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.ozonehis.eip.odooopenmrs.Constants;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
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

    public OdooClient(String url, String database, String username, String password) {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;
    }

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

    public Integer create(String model, List<Map<String, Object>> dataParams)
            throws XmlRpcException, MalformedURLException {
        authenticateIfNecessary();

        return (Integer) client.execute(
                "execute_kw", asList(getDatabase(), uid, getPassword(), model, Constants.CREATE_METHOD, dataParams));
    }

    public Boolean write(String model, List<Object> dataParams) throws XmlRpcException, MalformedURLException {
        authenticateIfNecessary();

        return (Boolean)
                client.execute("execute_kw", asList(getDatabase(), uid, getPassword(), model, "write", dataParams));
    }

    public Boolean delete(String model, List<Object> dataParams) throws XmlRpcException, MalformedURLException {
        authenticateIfNecessary();
        return (Boolean)
                client.execute("execute_kw", asList(getDatabase(), uid, getPassword(), model, "unlink", dataParams));
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
