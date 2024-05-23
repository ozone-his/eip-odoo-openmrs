/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.*;
import org.openmrs.eip.EIPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PartnerMapper implements ToOdooMapping<Patient, Partner> {

    private static final String ADDRESS_EXTENSION_URL = "http://fhir.openmrs.org/ext/address";

    private static final String ADDRESS1_EXTENSION = "http://fhir.openmrs.org/ext/address#address1";

    private static final String ADDRESS2_EXTENSION = "http://fhir.openmrs.org/ext/address#address2";
    private static final Logger log = LoggerFactory.getLogger(PartnerMapper.class);

    @Autowired
    private OdooClient odooClient;

    @Override
    public Partner toOdoo(Patient patient) {
        if (patient == null) {
            return null;
        }
        Partner partner = new Partner();
        partner.setPartnerRef(patient.getIdPart());
        partner.setPartnerActive(patient.getActive());
        // TODO: Gender not available in Odoo
        //        if (patient.hasGender()) {
        //            mapGender(patient.getGender()).ifPresent(customer::setGender);
        //        }
        String patientName = getPatientName(patient).orElse("");
        String patientIdentifier = getPreferredPatientIdentifier(patient).orElse("");
        partner.setPartnerName(patientName + " - " + patientIdentifier);

        addAddress(patient, partner);
        return partner;
    }

    protected Optional<String> getPreferredPatientIdentifier(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(identifier -> identifier.getUse() == Identifier.IdentifierUse.OFFICIAL)
                .findFirst()
                .map(Identifier::getValue);
    }

    protected Optional<String> getPatientName(Patient patient) {
        return patient.getName().stream()
                .findFirst()
                .map(name -> name.getGiven().get(0) + " " + name.getFamily());
    }

    protected void addAddress(Patient patient, Partner partner) {
        log.info(
                "PartnerMapper: Patient addAddress {},{},{}",
                patient.getAddress(),
                patient.getAddress().get(0).getUse(),
                patient.getAddress().get(0).getCity());
        if (patient.hasAddress()) {
            patient.getAddress().forEach(fhirAddress -> {
                partner.setPartnerCity(fhirAddress.getCity());
                partner.setPartnerCountryId(getCountryIdFromOdoo(fhirAddress.getCountry()));
                partner.setPartnerZip(fhirAddress.getPostalCode());
                partner.setPartnerStateId(getStateIdFromOdoo(fhirAddress.getPostalCode()));
                if (fhirAddress.getType() != null) {
                    partner.setPartnerType(fhirAddress.getType().getDisplay());
                }

                if (fhirAddress.hasExtension()) {
                    List<Extension> extensions = fhirAddress.getExtension();
                    List<Extension> addressExtensions = extensions.stream()
                            .filter(extension -> extension.getUrl().equals(ADDRESS_EXTENSION_URL))
                            .findFirst()
                            .map(Element::getExtension)
                            .orElse(new ArrayList<>());

                    addressExtensions.stream()
                            .filter(extension -> extension.getUrl().equals(ADDRESS1_EXTENSION))
                            .findFirst()
                            .ifPresent(extension -> partner.setPartnerStreet(
                                    extension.getValue().toString()));

                    addressExtensions.stream()
                            .filter(extension -> extension.getUrl().equals(ADDRESS2_EXTENSION))
                            .findFirst()
                            .ifPresent(extension -> partner.setPartnerStreet2(
                                    extension.getValue().toString()));
                }
            });
        }
    }

    private Integer getStateIdFromOdoo(String stateName) {
        try {
            Object[] records = odooClient.search(Constants.COUNTRY_STATE_MODEL, asList("name", "=", stateName));
            if (records.length > 1) {
                throw new EIPException(
                        String.format("Found %s states in odoo matching name: %s", records.length, stateName));
            } else if (records.length == 0) {
                log.warn("No state found in odoo matching name: {}", stateName);
                return null;
            }
            return (Integer) records[0];
        } catch (XmlRpcException | MalformedURLException e) {
            throw new RuntimeException("Error occurred while fetching state from Odoo", e);
        }
    }

    private Integer getCountryIdFromOdoo(String countryName) {
        try {
            Object[] records = odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName));
            if (records.length > 1) {
                throw new EIPException(
                        String.format("Found %s countries in odoo matching name: %s", records.length, countryName));
            } else if (records.length == 0) {
                log.warn("No country found in odoo matching name: {}", countryName);
                return null;
            }
            return (Integer) records[0];
        } catch (XmlRpcException | MalformedURLException e) {
            throw new RuntimeException(
                    String.format("Error occurred while fetching country from Odoo %s", e.getMessage()), e);
        }
    }
}
