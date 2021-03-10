INSERT INTO users (person_id,system_id,creator,date_created,retired,uuid)
VALUES (1, 'admin', 1, '2020-03-05 00:00:00', 1, '2b2636af-6b8c-4c6f-ad23-c5709c50fd40');

INSERT INTO person (gender,dead,birthdate_estimated,deathdate_estimated,creator,date_created,voided,uuid)
VALUES ('M', 0, 0, 0, 1, '2020-03-05 00:00:00', 0,'1d93d0cc-6534-48ed-bebc-4accda9471a5');

INSERT INTO patient (patient_id,creator,date_created,voided,allergy_status)
VALUES (1, 1, '2020-03-05 00:00:00', 0, 'Unknown');

INSERT INTO patient_identifier_type (name,required,check_digit,creator,date_created,retired,uuid)
VALUES ('Odoo Identifier', 0, 0, 1, '2020-03-05 00:00:00', 0, '7e93d0cc-6534-48ed-bebc-4beeda9471a5');

INSERT INTO patient_identifier (patient_id,identifier,identifier_type,preferred,creator,date_created,voided,uuid)
VALUES (1, '12345', 1, 1, 1, '2020-03-05 00:00:00', 0, '128bcfc0-360a-44a5-9539-e8718cd6e4d8');

INSERT INTO care_setting (name,care_setting_type,creator,date_created,retired,uuid)
VALUES ('Out-Patient', 'OUTPATIENT', 1, '2020-03-05 00:00:00', 1, '638bcfc0-360a-44a3-9539-e8718cd6e4d8');

INSERT INTO encounter_type (name,creator,date_created,retired,uuid)
VALUES ('Adult Initial', 1, '2020-03-05 00:00:00', 0, '1d93d0cc-6534-48ed-bebc-4accda9471a5');

INSERT INTO encounter (encounter_type,patient_id,encounter_datetime,creator,date_created,voided,uuid)
VALUES(1, 1, '2020-03-05 00:00:00', 1, '2020-03-05 00:00:00', 0, '5ade95d0-e095-43fc-b94f-7c585b7300f0');

INSERT INTO concept_datatype (name,creator,date_created,retired,uuid)
VALUES ('N/A', 1, '2020-03-05 00:00:00', 0, '4e6dcb16-d43e-46bb-b6bf-7088b9b82139');

INSERT INTO concept_class (name,creator,date_created,retired,uuid)
VALUES ('Cd4 Count', 1, '2020-03-05 00:00:00', 0, 'f4464518-f5e2-4aab-a54e-1f1a2ec6d431');

INSERT INTO concept (datatype_id,class_id,is_set,creator,date_created,retired,uuid)
VALUES (1, 1, 0, 1, '2020-03-05 00:00:00', 0, '945584a3-6c4a-4cb5-ba66-964aa9614239');

INSERT INTO provider (creator,date_created,retired,uuid)
VALUES (1, '2020-03-05 00:00:00', 0, '675584a3-6c4a-4cb5-ea66-964aa9614239');

INSERT INTO order_type (name,java_class_name,creator,date_created,retired,uuid)
VALUES('Test Order', 'org.openmrs.TestOrder', 1, '2020-03-05 00:00:00', 0, '2e93d0cc-6534-48ed-bebc-4aeeda9471a5');

INSERT INTO orders (order_type_id,encounter_id,concept_id,patient_id,urgency,order_number,order_action,care_setting,orderer,creator,date_activated,date_created,voided,uuid)
VALUES (1, 1, 1, 1, 'NO-URGENCY', 'ORD-1','NEW', 1, 1, 1, '2020-03-05 00:00:00', '2020-03-05 00:00:00', 0, '06170d8e-d201-4d94-ae89-0be0b0b6d8ba');