/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OdooFhirClient {
	
	private static OdooFhirClient instance;
	
	private static FhirContext fhirContext;
	
	@Value("${odoo.fhir.server.url}")
	private String serverUrl;
	
	@Value("${odoo.fhir.username}")
	private String username;
	
	@Value("${odoo.fhir.password}")
	private String password;
	
	private IGenericClient fhirClient;
	
	private OdooFhirClient() {
	}
	
	/**
	 * Gets an instance of this client.
	 * 
	 * @return the {@link OdooFhirClient} instance
	 */
	public static OdooFhirClient getInstance() {
		if (instance == null) {
			synchronized (OdooFhirClient.class) {
				if (instance == null) {
					fhirContext = FhirContext.forR4();
					fhirContext.getRestfulClientFactory().setConnectTimeout(30000);
					fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(120000);
					fhirContext.getRestfulClientFactory().setSocketTimeout(120000);
					instance = new OdooFhirClient();
				}
			}
		}
		
		return instance;
	}
	
	private IGenericClient getFhirClient() {
		if (fhirClient == null) {
			synchronized (this) {
				if (fhirClient == null) {
					fhirClient = fhirContext.newRestfulGenericClient(serverUrl + "/odoo/fhir/R4");
					fhirClient.registerInterceptor(new BasicAuthInterceptor(username, password));
				}
			}
		}
		
		return fhirClient;
	}
	
	/**
	 * Fetches all entities of the specified resource type
	 * 
	 * @param resourceType the resource type name
	 * @return Bundle of resources
	 */
	public Bundle getAll(Class<? extends IBaseResource> resourceType) {
		if (log.isDebugEnabled()) {
			log.debug("Getting all resources of type {}", resourceType);
		}
		
		return getFhirClient().search().forResource(resourceType).returnBundle(Bundle.class).execute();
	}
	
}
