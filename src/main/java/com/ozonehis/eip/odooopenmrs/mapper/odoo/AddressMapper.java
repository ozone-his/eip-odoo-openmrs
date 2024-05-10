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
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AddressMapper implements ToOdooMapping<org.hl7.fhir.r4.model.Address, Address> {

    private static final String ADDRESS_TYPE = "Personal";

    private static final String ADDRESS_EXTENSION_URL = "http://fhir.openmrs.org/ext/address";

    private static final String ADDRESS1_EXTENSION = "http://fhir.openmrs.org/ext/address#address1";

    private static final String ADDRESS2_EXTENSION = "http://fhir.openmrs.org/ext/address#address2";

    @Override
    public Address toOdoo(org.hl7.fhir.r4.model.Address fhirAddress) {
        Address address = new Address();
        address.setAddressContactAddress(fhirAddress.getIdElement().getValue());//TODO: Check if correct
        address.setAddressCity(fhirAddress.getCity());
        Country country = new Country();
        country.setCountryName(fhirAddress.getCountry());
        address.setCountry(country);
        address.setAddressZip(fhirAddress.getPostalCode());
        CountryState countryState = new CountryState();
        countryState.setCountryStateName(fhirAddress.getState());
        address.setCountryState(countryState);
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
                            address.setAddressStreet(extension.getValue().toString()));

            addressExtensions.stream()
                    .filter(extension -> extension.getUrl().equals(ADDRESS2_EXTENSION))
                    .findFirst()
                    .ifPresent(extension ->
                            address.setAddressStreet2(extension.getValue().toString()));
        }

        if (fhirAddress.hasUse()) {
            // TODO: Check if this is the correct way to map the address type
            if (fhirAddress.getUse().equals(org.hl7.fhir.r4.model.Address.AddressUse.HOME)) {
                // address.setAddressType(ADDRESS_TYPE); TODO: Not available in Odoo
            }
        }

        return address;
    }
}
