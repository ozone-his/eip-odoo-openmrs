/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.camel.test.infra.odoo.services;

import lombok.NoArgsConstructor;
import org.apache.camel.test.infra.common.services.SimpleTestServiceBuilder;
import org.apache.camel.test.infra.common.services.SingletonService;

@NoArgsConstructor
public class OdooServiceFactory {

    static class SingletonOdooService extends SingletonService<OdooService> implements OdooService {

        public SingletonOdooService(OdooService service, String name) {
            super(service, name);
        }

        @Override
        public int getPort() {
            return getService().getPort();
        }

        public String getHost() {
            return getService().getHost();
        }

        @Override
        public String getHttpHostAddress() {
            return getService().getHttpHostAddress();
        }
    }

    public static SimpleTestServiceBuilder<OdooService> builder() {
        return new SimpleTestServiceBuilder<>("odoo");
    }

    public static OdooService createService() {
        return builder().addLocalMapping(OdooLocalContainerService::new).build();
    }

    public static OdooService createSingletonService() {
        return SingletonServiceHolder.INSTANCE;
    }

    private static class SingletonServiceHolder {

        static final OdooService INSTANCE;

        static {
            SimpleTestServiceBuilder<OdooService> instance = builder();
            instance.addLocalMapping(() -> new SingletonOdooService(new OdooLocalContainerService(), "odoo"));
            INSTANCE = instance.build();
        }
    }
}
