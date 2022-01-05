package com.mekomsolutions.eip;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class OdooApp {
	
	final static String db = "odoo";
	
	final static String uid = "2";
	
	final static String password = "q1";//odoo
	
	public static void main(final String[] args) throws Exception {
		System.out.println("Starting Odoo App...");
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL("http://127.0.0.1:8069/xmlrpc/2/object"));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		
		//getModelFields(client, "ir.model");
		//getModelFields(client, "ir.ui.view");
		//getRecord(client, "res.partner", 7);
		//getModel(client, "res.partner");
		//addField(client, 79, "x_emr_id", "char", true, "EMR Identifier");
		//getRecord(client, "ir.ui.view", 118);
	}
	
	private static void getModelFields(XmlRpcClient client, String model) throws Exception {
		final Map<String, Object> fields = (Map) client.execute("execute_kw",
		    asList(db, uid, password, model, "fields_get", emptyList(), new HashMap<String, Object>() {
			    
			    {
				    //put("attributes", asList("string", "help", "type"));
			    }
			    
		    }));
		
		Map treeMap = new TreeMap(fields);
		System.out.println(treeMap);
	}
	
	private static void getRecord(XmlRpcClient client, String model, int id) throws Exception {
		Object[] record = (Object[]) client.execute("execute_kw",
		    asList(db, uid, password, model, "search_read", asList(asList(asList("id", "=", id))), new HashMap() {
			    
			    {
				    //put("fields", asList("name", "comment", "company_type", "is_company",
				    //    "company_type", "x_emr_id"));
				    //put("limit", 5);
			    }
		    }));
		
		List<Object> list = new ArrayList(record.length);
		for (Object o : Arrays.asList(record)) {
			list.add(o);
		}
		
		System.out.println(list);
	}
	
	private static void getModel(XmlRpcClient client, String name) throws Exception {
		Object[] model = (Object[]) client.execute("execute_kw",
		    asList(db, uid, password, "ir.model", "search_read", asList(asList(asList("model", "=", name)))));
		
		List<Object> list = new ArrayList(model.length);
		for (Object o : Arrays.asList(model)) {
			list.add(o);
		}
		
		System.out.println(list);
	}
	
	private static void addField(XmlRpcClient client, int modelId, String name, String type, boolean required,
	                             String description)
	    throws Exception {
		Object resp = client.execute("execute_kw",
		    asList(db, uid, password, "ir.model.fields", "create", asList(new HashMap<String, Object>() {
			    
			    {
				    put("model_id", modelId);
				    put("name", name);
				    put("field_description", description);
				    put("ttype", type);
				    put("state", "manual");
				    put("required", required);
			    }
		    })));
		
		System.out.println("Field Id:" + resp);
	}
	
}
