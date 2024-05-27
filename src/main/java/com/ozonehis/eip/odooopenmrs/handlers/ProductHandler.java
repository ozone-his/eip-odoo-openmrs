package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.model.Product;
import java.net.MalformedURLException;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ProductHandler {

    @Autowired
    private OdooClient odooClient;

    public Product getProduct(Resource resource) {
        String externalId = getProductExternalId(resource);
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.IR_MODEL,
                    asList(asList("model", "=", Constants.PRODUCT_MODEL), asList("name", "=", externalId)),
                    null);
            if (records == null) {
                throw new EIPException(
                        String.format("Got null response while fetching for Product with id %s", externalId));
            } else if (records.length == 1) {
                log.info("Product exists with id {} record {}", externalId, records[0]);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Product.class);
            } else if (records.length == 0) {
                log.info("No Product found with id {}", externalId);
                throw new EIPException(String.format("No Product found with id %s", externalId));
            } else {
                throw new EIPException(String.format("Multiple Products exists with id %s", externalId));
            }
        } catch (MalformedURLException | XmlRpcException e) {
            log.error("Error occurred while fetching product with id {} error {}", externalId, e.getMessage(), e);
            throw new CamelExecutionException("Error occurred while fetching product", null, e);
        }
    }

    private String getProductExternalId(Resource resource) {
        if (resource instanceof ServiceRequest serviceRequest) {
            String id = serviceRequest.getCode().getCodingFirstRep().getCode();
            log.info("ProductHandler: ServiceRequest code {}", id);
            return id;
        } else if (resource instanceof MedicationRequest medicationRequest) {
            String id =
                    medicationRequest.getMedicationReference().getReference().split("/")[1];
            log.info("ProductHandler: MedicationRequest reference {}", id);
            return id;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported resource type: " + resource.getClass().getName());
        }
    }
}
