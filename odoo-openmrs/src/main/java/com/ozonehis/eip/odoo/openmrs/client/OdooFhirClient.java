/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OdooFhirClient {

    @Value("${odoo.fhir.server.url}")
    private String serverUrl;

    @Value("${odoo.fhir.username}")
    private String username;

    @Value("${odoo.fhir.password}")
    private String password;

    private FhirContext fhirContext;

    private IGenericClient fhirClient;

    private OdooFhirClient() {}

    private static final class InstanceHolder {

        private static final OdooFhirClient INSTANCE = new OdooFhirClient();
    }

    /**
     * Gets an instance of this client.
     *
     * @return the {@link OdooFhirClient} instance
     */
    public static OdooFhirClient getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private IGenericClient getFhirClient() {
        if (fhirClient == null) {
            synchronized (this) {
                if (fhirClient == null) {
                    fhirContext = FhirContext.forR4();
                    fhirContext.getRestfulClientFactory().setConnectTimeout(30000);
                    fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(120000);
                    fhirContext.getRestfulClientFactory().setSocketTimeout(120000);
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

        return getFhirClient()
                .search()
                .forResource(resourceType)
                .returnBundle(Bundle.class)
                .execute();
    }
}
