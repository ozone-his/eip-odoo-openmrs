# EIP Odoo OpenMRS

Apache camel routes that integrate Odoo and OpenMRS.

## Table of Contents

1. [Technical Overview](#technical-overview)
2. [Dependencies](#dependencies)
3. [Integrations](#integrations)
   1. [Observations](#observation-based-integration)
   2. [Orders](#order-based-integration)
4. [Installation](#installation)
   1. [PRP Integration](docs/prp/README.md)
   2. [Orders Based Integration](docs/orders/README.md)

### Technical Overview

The project is built on top of [OpenMRS-EIP core](https://github.com/openmrs/openmrs-eip), so the assumption is that
you have an existing OpenMRS EIP based application and wish to add to it odoo integration logic. The project contains
camel routes that track inserts, updates and delete operations in specific tables in an OpenMRS database to take
appropriate action in an odoo system.

If you don't have an existing OpenMRS EIP based application, you will need to first create one as
[documented here](https://github.com/openmrs/openmrs-eip/tree/master/docs/custom), then add the camel routes
provided in this project and application properties as documented under [installation](#installation).

### Dependencies

You will need to include the following dependencies on the classpath in a deployment environment of your OpenMRS based
application,

[xmlrpc-client 3.1.3](https://mvnrepository.com/artifact/org.apache.xmlrpc/xmlrpc-client/3.1.3)

### Integrations

The current integration logic only supports tracking orders, observations database events (inserts, updates, deletes)
in OpenMRS in order to take some appropriate action in the odoo instance.

All integrations involve tracking database inserts, updates and deletes in the common tables below containing patient
demographic data,
1. patient
2. person_name
3. person_address
4. patient_identifier

When a database insert, update or delete event is received from the above tables, the camel routes take the follow
actions,
1. If the patient is not voided, a customer record is created in odoo for the associated patient if it doesn't exist
otherwise the existing customer record gets updated.
2. If the patient is voided and, they have a customer record in odoo, the odoo record is archived.
3. If it is a patient delete event and, the associated patient has a customer record in odoo, the odoo record is
archived and if they have any existing quotations they get cancelled.

See below for more details for extra tables for each integration.

#### Observation Based Integration

The routes for obs based integration involves tracking database inserts, updates and deletes in the extra tables below,
1. obs

When a database insert or update event is received from the obs table and, the obs is not voided, the camel routes
create a customer record in odoo for the associated patient if it doesn't exist.

When a database insert or update event is received from the obs table and, the obs is not voided and has the expected
question and answer, the camel routes create a customer record in odoo for the associated patient if they don't exist
yet otherwise the existing record is updated.

#### Order Based Integration

The routes for orders based integration involves tracking database inserts, updates and deletes in the tables below,
1. orders
2. test_order
3. drug_order

When a database insert or update event is received from any of the above 3 tables, the camel routes do the following,
1. For a new or revision order that is not voided, a customer record gets created in odoo for the associated patient if
none exists, a quotation is started for the patient if none exists, the item(order line) is added to the quotation
for the ordered item if none exists. In case of a revision of a drug order and, the quantity has changed, the quantity
for the item gets updated on the existing quotation.
2. If the order is voided or is a Discontinuation order, and the associated patient doesn't exist in odoo, the event is
ignored.
3. If the order is voided or is a Discontinuation order, and the associated patient has an active quotation in odoo
that was created by this integration i.e. the creator of the quotation matches the odoo user configured for the
integration, the item(order line) gets removed from the quotation and if the quotation is left with no more items,
it gets cancelled.

### Installation

1. [PRP Integration](docs/prp/README.md)
2. [Orders Based Integration](docs/orders/README.md)

