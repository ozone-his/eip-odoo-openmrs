INSERT INTO users (user_id,person_id,system_id,creator,date_created,retired,uuid)
VALUES  (1, 1, 'admin', 1, '2020-03-05 00:00:00', 1, '2b2636af-6b8c-4c6f-ad23-c5709c50fd40');

INSERT INTO person (person_id,gender,dead,birthdate_estimated,deathdate_estimated,creator,date_created,voided,uuid)
VALUES  (1, 'M', 0, 0, 0, 1, '2020-03-05 00:00:00', 0,'ba3b12d1-5c4f-415f-871b-b98a22137604'),
        (2, 'F', 0, 0, 0, 1, '2020-03-05 00:00:00', 0,'2d93d0cc-6534-48ed-bebc-4accda9471a5');

INSERT INTO person_name (person_name_id,person_id,given_name,family_name,preferred,creator,date_created,voided,uuid)
VALUES  (1, 1, 'John', 'Doe', 1, 1, '2020-03-05 00:00:00', 0, '0bca417f-fc68-40d7-ae6f-cffca7a5eff1'),
        (2, 2, 'Mary', 'Jane', 1, 1, '2020-03-05 00:00:00', 0, '448bcfc0-360a-44b5-9539-e8718cd6e46e');

INSERT INTO person_address (person_address_id,person_id,address1,address2,city_village,state_province,postal_code,country,preferred,creator,date_created,voided,uuid)
VALUES  (1, 1, '25 Ocean Drive', 'Apt 1', 'Fort Lauderdale', 'FL', '33301', 'United States', 1, 1, '2020-03-05 00:00:00', 0, '359022bf-4a58-4732-8cce-1e57f72f47b0');

INSERT INTO patient (patient_id,creator,date_created,voided,allergy_status)
VALUES  (1, 1, '2020-03-05 00:00:00', 0, 'Unknown'),
        (2, 1, '2020-03-05 00:00:00', 0, 'Unknown');

INSERT INTO patient_identifier_type (patient_identifier_type_id,name,required,check_digit,creator,date_created,retired,uuid)
VALUES  (1, 'OpenMRS Id', 0, 0, 1, '2020-03-05 00:00:00', 0, '6e93d0cc-6534-48ed-bebc-4beeda9471a5');


INSERT INTO patient_identifier (patient_identifier_id,patient_id,identifier,identifier_type,preferred,creator,date_created,voided,uuid)
VALUES  (1, 1, '12345', 1, 1, 1, '2020-03-05 00:00:00', 0, '128bcfc0-360a-44a5-9539-e8718cd6e4d8'),
        (2, 2, 'QWERT', 1, 1, 1, '2020-03-05 00:00:00', 0, '228bcfc0-360a-44a5-9539-e8718cd6e4d8');

INSERT INTO care_setting (care_setting_id,name,care_setting_type,creator,date_created,retired,uuid)
VALUES  (1, 'Out-Patient', 'OUTPATIENT', 1, '2020-03-05 00:00:00', 1, '638bcfc0-360a-44a3-9539-e8718cd6e4d8');

INSERT INTO encounter_type (encounter_type_id,name,creator,date_created,retired,uuid)
VALUES  (1, 'Adult Initial', 1, '2020-03-05 00:00:00', 0, '1d93d0cc-6534-48ed-bebc-4accda9471a5');

INSERT INTO encounter (encounter_id,encounter_type,patient_id,encounter_datetime,creator,date_created,voided,uuid)
VALUES  (1, 1, 1, '2020-03-05 00:00:00', 1, '2020-03-05 00:00:00', 0, '5ade95d0-e095-43fc-b94f-7c585b7300f0'),
        (2, 1, 2, '2020-03-05 00:00:00', 1, '2020-03-05 00:00:00', 0, '6ade95d0-e095-43fc-b94f-7c585b7300f0');

INSERT INTO concept_datatype (concept_datatype_id, name,creator,date_created,retired,uuid)
VALUES  (1, 'N/A', 1, '2020-03-05 00:00:00', 0, '4e6dcb16-d43e-46bb-b6bf-7088b9b82139');

INSERT INTO concept_class (concept_class_id,name,creator,date_created,retired,uuid)
VALUES  (1, 'Finding', 1, '2020-03-05 00:00:00', 0, 'f4464518-f5e2-4aab-a54e-1f1a2ec6d431'),
        (2, 'Units Of Measure', 1, '2020-03-05 00:00:00', 0, 'e30d8601-07f8-413a-9d11-cdfbb28196ec');

INSERT INTO concept (concept_id,datatype_id,class_id,is_set,creator,date_created,retired,uuid)
VALUES  (1, 1, 1, 0, 1, '2020-03-05 00:00:00', 0, '945584a3-6c4a-4cb5-ba66-964aa9614239'),
        (2, 1, 2, 0, 1, '2020-03-05 00:00:00', 0, '845584a3-6c4a-4cb5-ba66-964aa9614239'),
        (3, 1, 2, 0, 1, '2020-03-05 00:00:00', 0, '745584a3-6c4a-4cb5-ba66-964aa9614239');

INSERT INTO drug (drug_id,concept_id,combination,creator,date_created,retired,uuid)
VALUES (1, 1, 0, 1, '2020-03-05 00:00:00', 0, '1a93d0dd-6534-48ed-bebc-4aeeda9471e6');

INSERT INTO provider (provider_id, creator,date_created,retired,uuid)
VALUES  (1, 1, '2020-03-05 00:00:00', 0, '675584a3-6c4a-4cb5-ea66-964aa9614239');

INSERT INTO order_type (order_type_id, name,java_class_name,creator,date_created,retired,uuid)
VALUES  (1, 'Test Order', 'org.openmrs.TestOrder', 1, '2020-03-05 00:00:00', 0, '2e93d0cc-6534-48ed-bebc-4aeeda9471a5');

INSERT INTO orders (order_id,order_type_id,patient_id,encounter_id,concept_id,urgency,order_number,order_action,care_setting,orderer,previous_order_id,creator,date_activated,date_created,voided,uuid)
VALUES  (1, 1, 2, 1, 1, 'NO-URGENCY', 'ORD-1','NEW', 1, 1, null, 1, '2020-03-05 00:00:00', '2020-03-05 00:00:00', 0, '16170d8e-d201-4d94-ae89-0be0b0b6d8ba'),
        (2, 1, 1, 2, 1, 'NO-URGENCY', 'ORD-2','NEW', 1, 1, null, 1, '2020-03-05 00:00:00', '2020-03-05 00:00:00', 0, '26170d8e-d201-4d94-ae89-0be0b0b6d8ba'),
        (3, 1, 1, 2, 1, 'NO-URGENCY', 'ORD-3','REVISE', 1, 1, 2, 1, '2020-03-05 00:00:00', '2020-03-05 00:00:00', 0, '36170d8e-d201-4d94-ae89-0be0b0b6d8ba'),
        (4, 1, 1, 2, 1, 'NO-URGENCY', 'ORD-4','DISCONTINUE', 1, 1, 3, 1, '2020-03-05 00:00:00', '2020-03-05 00:00:00', 0, '46170d8e-d201-4d94-ae89-0be0b0b6d8ba');

INSERT INTO test_order (order_id)
VALUES  (1);

INSERT INTO drug_order (order_id,drug_inventory_id,dispense_as_written,quantity,quantity_units)
VALUES  (2, 1, 0, 2.0, 2),
        (3, 1, 0, 3.0, 3);