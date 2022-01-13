select * from patient;
select * from patient_identifier_type;
select * from patient_identifier;
select * from person;
select * from person_name;
select * from person_address;

update patient set date_created = now() where patient_id =1;
update person_name set date_created = now() where person_name_id =1;
update person_address set date_created = now() where person_address_id =1;

select * from obs;
select * from orders;
select * from drug_order;
select * from concept;
select * from concept_name;
select * from encounter_type;
select * from encounter;
select * from provider;
select * from order_frequency;

insert into obs(person_id,concept_id,value_coded,obs_datetime,status,creator,date_created,voided,uuid)
values((select person_id from person where uuid='person-uuid-1'), 4, 5, now(), 'FINAL', 1, now(), 0, 'obs-uuid-1');


-- Start Patient record and Metadata
insert into person(gender,creator,date_created,voided,uuid) values('M', 1, now(), 0, 'person-uuid-1');
INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,voided,uuid)
VALUES  ((select person_id from person where uuid='person-uuid-1'), 'John', 'Doe', 1, now(), 0, 'person_name_uuid-1');

insert into patient(patient_id,creator,date_created,voided,allergy_status)
values ((select person_id from person where uuid='person-uuid-1'), 1, now(), 0,'Unknown');

INSERT INTO patient_identifier (patient_id,identifier,identifier_type,preferred,location_id,creator,date_created,voided,uuid)
VALUES  ((select person_id from person where uuid='person-uuid-1'), '12345', 2, 1, null, 1, now(), 0, 'patient-id-uuid-1');

insert into encounter_type(name,creator,date_created,retired,uuid) values('Adult Initial', 1, now(), 0, 'enc-type-uuid-1');

insert into encounter(encounter_type,patient_id,encounter_datetime,creator,date_created,voided,uuid)
values((select encounter_type_id from encounter_type where uuid='enc-type-uuid-1'), (select person_id from person where uuid='person-uuid-1'), now(), 1, now(), 0, 'enc-uuid-1');

insert into concept(datatype_id,class_id,creator,date_created,retired,uuid)
values (4, 11, 1, now(), 0, 'concept-uuid-1'); -- dose units

insert into concept_name (concept_id,name,locale,creator,date_created,voided,uuid)
values ((select concept_id from concept where uuid='concept-uuid-1'), 'Tabs', 'en', 1, now(), 0, 'concept-name-uuid-1');

insert into concept(datatype_id,class_id,creator,date_created,retired,uuid)
values (4, 11, 1, now(), 0, 'concept-uuid-2'); -- route

insert into concept_name (concept_id,name,locale,creator,date_created,voided,uuid)
values ((select concept_id from concept where uuid='concept-uuid-2'), 'Mouth', 'en', 1, now(), 0, 'concept-name-uuid-2');


insert into concept(datatype_id,class_id,creator,date_created,retired,uuid)
values (4, 11, 1, now(), 0, 'concept-uuid-3'); -- qty

insert into concept(datatype_id,class_id,creator,date_created,retired,uuid)
values (4, 11, 1, now(), 0, 'concept-uuid-4'); -- frequency

insert into concept(datatype_id,class_id,creator,date_created,retired,uuid)
values (4, 11, 1, now(), 0, 'concept-uuid-5'); -- drug

insert into concept(datatype_id,class_id,is_set,creator,date_created,retired,uuid)
values (4, 11, 1, 1, now(), 0, 'concept-uuid-6'); -- routes set

insert into concept(datatype_id,class_id,is_set,creator,date_created,retired,uuid)
values (4, 11, 1, 1, now(), 0, 'concept-uuid-7'); -- dose units set

insert into concept(datatype_id,class_id,is_set,creator,date_created,retired,uuid)
values (4, 11, 1, 1, now(), 0, 'concept-uuid-8'); -- qty units set

insert into concept_set (concept_id,concept_set,creator,date_created,uuid)
values ((select concept_id from concept where uuid='concept-uuid-1'), (select concept_id from concept where uuid='concept-uuid-7'), 1, now(), 'set-uuid-1');

insert into concept_set (concept_id,concept_set,creator,date_created,uuid)
values ((select concept_id from concept where uuid='concept-uuid-2'), (select concept_id from concept where uuid='concept-uuid-6'), 1, now(), 'set-uuid-2');

insert into concept_set (concept_id,concept_set,creator,date_created,uuid)
values ((select concept_id from concept where uuid='concept-uuid-3'), (select concept_id from concept where uuid='concept-uuid-8'), 1, now(), 'set-uuid-3');

insert into provider(person_id,creator,date_created,retired,uuid)
values (1, 1, now(), 0, 'provider-uuid-1');

insert into order_frequency(frequency_per_day,concept_id,creator,date_created,retired,uuid)
values (4, (select concept_id from concept where uuid='concept-uuid-4'), 1, now(), 0, 'frequency-uuid-1');

INSERT INTO drug (drug_id,concept_id,combination,creator,date_created,retired,uuid)
VALUES (1, (select concept_id from concept where uuid='concept-uuid-5'), 0, 1, now(), 0, 'drug-uuid-1');

update global_property set property_value = 'concept-uuid-6' where property = 'order.drugRoutesConceptUuid';
update global_property set property_value = 'concept-uuid-7' where property = 'order.drugDosingUnitsConceptUuid';
update global_property set property_value = 'concept-uuid-8' where property = 'order.drugDispensingUnitsConceptUuid';
-- End Patient record and Metadata
