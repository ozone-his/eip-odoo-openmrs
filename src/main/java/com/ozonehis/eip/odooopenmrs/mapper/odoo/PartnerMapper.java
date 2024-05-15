/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import com.ozonehis.eip.odooopenmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odooopenmrs.model.Address;
import com.ozonehis.eip.odooopenmrs.model.Country;
import com.ozonehis.eip.odooopenmrs.model.CountryState;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PartnerMapper implements ToOdooMapping<Patient, Partner> {

    private static final String ADDRESS_EXTENSION_URL = "http://fhir.openmrs.org/ext/address";

    private static final String ADDRESS1_EXTENSION = "http://fhir.openmrs.org/ext/address#address1";

    private static final String ADDRESS2_EXTENSION = "http://fhir.openmrs.org/ext/address#address2";

    @Override
    public Partner toOdoo(Patient patient) {
        if (patient == null) {
            return null;
        }
        Partner partner = new Partner();
        // TODO: Check if getIdPart is same as uuid
        partner.setPartnerRef(patient.getIdPart());
        // TODO: Gender not available in Odoo
//        if (patient.hasGender()) {
//            mapGender(patient.getGender()).ifPresent(customer::setGender);
//        }
        String patientName = getPatientName(patient).orElse("");
        String patientIdentifier = getPreferredPatientIdentifier(patient).orElse("");

        partner.setPartnerName(patientName + " - " + patientIdentifier);
        // TODO: Check if odoo contains CustomerType like variable
//        customer.setCustomerType(CustomerType.INDIVIDUAL);

        addAddress(patient, partner);
        return partner;
    }

//    protected Optional<ERPNextGender> mapGender(Enumerations.AdministrativeGender gender) {
//        switch (gender) {
//            case MALE -> {
//                return Optional.of(ERPNextGender.MALE);
//            }
//            case FEMALE -> {
//                return Optional.of(ERPNextGender.FEMALE);
//            }
//            case OTHER -> {
//                return Optional.of(ERPNextGender.OTHER);
//            }
//            default -> {
//                return Optional.empty();
//            }
//        }
//    }

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
        if (patient.hasAddress()) {
            patient.getAddress().stream()
                    .filter(a -> a.getUse() != org.hl7.fhir.r4.model.Address.AddressUse.HOME)
                    .forEach(fhirAddress -> {
                        partner.setPartnerContactAddress(fhirAddress.getIdElement().getValue());//TODO: Check if correct
                        partner.setPartnerCity(fhirAddress.getCity());
                        Country country = new Country();
                        country.setCountryName(fhirAddress.getCountry());
                        partner.setCountry(country);
                        partner.setPartnerZip(fhirAddress.getPostalCode());
                        CountryState countryState = new CountryState();
                        countryState.setCountryStateName(fhirAddress.getState());
                        partner.setCountryState(countryState);
                        //        address.setPrimaryAddress(true); TODO: Not available in Odoo

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
                                    .ifPresent(extension ->
                                            partner.setPartnerStreet(extension.getValue().toString()));

                            addressExtensions.stream()
                                    .filter(extension -> extension.getUrl().equals(ADDRESS2_EXTENSION))
                                    .findFirst()
                                    .ifPresent(extension ->
                                            partner.setPartnerStreet2(extension.getValue().toString()));
                        }

                        if (patient.hasTelecom()) {
                            partner.setPartnerPhone(patient.getTelecomFirstRep().getValue());
                        }

                        if (fhirAddress.hasUse()) {
                            // TODO: Check if this is the correct way to map the address type
                            if (fhirAddress.getUse().equals(org.hl7.fhir.r4.model.Address.AddressUse.HOME)) {
                                // address.setAddressType(ADDRESS_TYPE); TODO: Not available in Odoo
                            }
                        }
                    });
        }
    }
}
