/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper;

import org.hl7.fhir.r4.model.Resource;

/**
 * An Interface for mapping from FHIR Resources to Odoo Resources
 *
 * @param <F> FHIR Resource
 * @param <E> Odoo Resource
 */
public interface ToFhirMapping<E, F extends Resource> {

    /**
     * Maps an Odoo Resource to a FHIR Resource
     *
     * @param odooResource Odoo Resource
     * @return FHIR Resource
     */
    F toFhir(E odooResource);
}
