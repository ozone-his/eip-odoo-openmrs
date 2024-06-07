/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.component.OdooComponent;
import com.ozonehis.eip.odooopenmrs.handlers.CountryHandler;
import com.ozonehis.eip.odooopenmrs.handlers.CountryStateHandler;
import com.ozonehis.eip.odooopenmrs.handlers.PartnerHandler;
import com.ozonehis.eip.odooopenmrs.handlers.ProductHandler;
import com.ozonehis.eip.odooopenmrs.handlers.SaleOrderHandler;
import com.ozonehis.eip.odooopenmrs.handlers.SaleOrderLineHandler;
import com.ozonehis.eip.odooopenmrs.handlers.UomHandler;
import com.ozonehis.eip.odooopenmrs.integration.infra.OdooService;
import com.ozonehis.eip.odooopenmrs.integration.infra.OdooServiceFactory;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odooopenmrs.processors.MedicationRequestProcessor;
import com.ozonehis.eip.odooopenmrs.processors.PatientProcessor;
import com.ozonehis.eip.odooopenmrs.processors.ServiceRequestProcessor;
import com.ozonehis.eip.odooopenmrs.routes.MedicationRequestRouting;
import com.ozonehis.eip.odooopenmrs.routes.PatientRouting;
import com.ozonehis.eip.odooopenmrs.routes.ServiceRequestRouting;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.CamelContextExtension;
import org.apache.camel.test.infra.core.DefaultCamelContextExtension;
import org.apache.camel.test.infra.core.annotations.ContextFixture;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Getter
@ActiveProfiles("test")
@CamelSpringBootTest
@SpringBootTest(classes = {TestSpringConfiguration.class})
public abstract class BaseRouteIntegrationTest {

    private OdooClient odooClient;

    private static final String ODOO_SERVER_URL = "http://localhost:8069";

    private static final String ODOO_DATABASE = "odoo";

    private static final String ODOO_USERNAME = "admin";

    private static final String ODOO_PASSWORD = "admin";

    @RegisterExtension
    protected static CamelContextExtension contextExtension = new DefaultCamelContextExtension();

    @RegisterExtension
    protected static final OdooService odooService = OdooServiceFactory.createSingletonService();

    @ContextFixture
    public void configureContext(CamelContext context) {
        context.getComponent("odoo", OdooComponent.class).setOdooClient(getOdooClient());
    }

    protected static OdooClient createOdooClient() {
        return new OdooClient(ODOO_SERVER_URL, ODOO_DATABASE, ODOO_USERNAME, ODOO_PASSWORD);
    }

    public OdooClient getOdooClient() {
        if (odooClient == null) {
            odooClient = createOdooClient();
        }
        return odooClient;
    }

    protected @Nonnull CamelContext getContextWithRouting(CamelContext context) throws Exception {
        CountryHandler countryHandler = new CountryHandler();
        countryHandler.setOdooClient(getOdooClient());

        CountryStateHandler countryStateHandler = new CountryStateHandler();
        countryStateHandler.setOdooClient(getOdooClient());

        SaleOrderLineMapper<Resource> saleOrderLineMapper = new SaleOrderLineMapper<>();

        UomHandler uomHandler = new UomHandler();
        uomHandler.setOdooClient(getOdooClient());

        ProductHandler productHandler = new ProductHandler();
        productHandler.setOdooClient(getOdooClient());

        SaleOrderMapper saleOrderMapper = new SaleOrderMapper();

        SaleOrderLineHandler saleOrderLineHandler = new SaleOrderLineHandler();
        saleOrderLineHandler.setOdooClient(getOdooClient());
        saleOrderLineHandler.setProductHandler(productHandler);
        saleOrderLineHandler.setUomHandler(uomHandler);
        saleOrderLineHandler.setSaleOrderLineMapper(saleOrderLineMapper);

        PartnerMapper partnerMapper = new PartnerMapper();
        partnerMapper.setCountryHandler(countryHandler);
        partnerMapper.setCountryStateHandler(countryStateHandler);

        PartnerHandler partnerHandler = new PartnerHandler();
        partnerHandler.setOdooClient(getOdooClient());
        partnerHandler.setPartnerMapper(partnerMapper);

        SaleOrderHandler saleOrderHandler = new SaleOrderHandler();
        saleOrderHandler.setOdooClient(getOdooClient());
        saleOrderHandler.setSaleOrderLineHandler(saleOrderLineHandler);
        saleOrderHandler.setSaleOrderMapper(saleOrderMapper);
        saleOrderHandler.setProductHandler(productHandler);

        PatientProcessor patientProcessor = new PatientProcessor();
        patientProcessor.setPartnerHandler(partnerHandler);
        patientProcessor.setMapper(partnerMapper);

        PatientRouting patientRouting = new PatientRouting();
        patientRouting.setPatientProcessor(patientProcessor);
        patientRouting.setPatientSyncEnabled(true);

        MedicationRequestProcessor medicationRequestProcessor = new MedicationRequestProcessor();
        medicationRequestProcessor.setSaleOrderHandler(saleOrderHandler);
        medicationRequestProcessor.setPartnerHandler(partnerHandler);

        MedicationRequestRouting medicationRequestRouting = new MedicationRequestRouting();
        medicationRequestRouting.setMedicationRequestProcessor(medicationRequestProcessor);

        ServiceRequestProcessor serviceRequestProcessor = new ServiceRequestProcessor();
        serviceRequestProcessor.setSaleOrderHandler(saleOrderHandler);
        serviceRequestProcessor.setPartnerHandler(partnerHandler);

        ServiceRequestRouting serviceRequestRouting = new ServiceRequestRouting();
        serviceRequestRouting.setServiceRequestProcessor(serviceRequestProcessor);

        context.addRoutes(patientRouting);
        context.addRoutes(medicationRequestRouting);
        context.addRoutes(serviceRequestRouting);

        return context;
    }

    protected boolean hasRoute(CamelContext context, String routeId) {
        return context.getRoute(routeId) != null;
    }

    /**
     * Send a body and headers to an endpoint.
     *
     * @param endpoint the endpoint to send the body to.
     * @param body     the body to send.
     * @param headers  the headers to send.
     */
    protected void sendBodyAndHeaders(String endpoint, Object body, Map<String, Object> headers) {
        contextExtension
                .getProducerTemplate()
                .sendBodyAndHeaders(contextExtension.getContext().getEndpoint(endpoint), body, headers);
    }

    /**
     * Load resource from a file path.
     *
     * @param filePath the file path of the resource to load.
     * @param resource resource object
     * @param <T>      The type of the resource to load e.g., Patient, Encounter, etc.
     * @return resource object
     */
    @SuppressWarnings("unchecked")
    protected <T extends Resource> T loadResource(String filePath, T resource) {
        FhirContext ctx = FhirContext.forR4();
        return (T) ctx.newJsonParser().parseResource(resource.getClass(), readJSON(filePath));
    }

    /**
     * Read JSON file from the classpath.
     *
     * @param filePath the file path of the JSON file to read.
     * @return JSON content as a string
     */
    protected String readJSON(String filePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found! " + filePath);
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
