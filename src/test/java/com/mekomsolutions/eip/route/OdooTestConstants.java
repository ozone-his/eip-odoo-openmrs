package com.mekomsolutions.eip.route;

public class OdooTestConstants {
	
	public static final String EX_PROP_TABLE_RESOURCE_MAP = "tables-resource-map";
	
	public static final String EX_PROP_ENTITY = "entity-instance";
	
	public static final String EX_PROP_IS_SUBRESOURCE = "isSubResource";
	
	public static final String EX_PROP_RESOURCE_NAME = "resourceName";
	
	public static final String EX_PROP_RESOURCE_ID = "resourceId";
	
	public static final String EX_PROP_SUB_RESOURCE_NAME = "subResourceName";
	
	public static final String EX_PROP_SUB_RESOURCE_ID = "subResourceId";
	
	public static final String EX_PROP_RES_REP = "resourceRepresentation";
	
	public static final String EX_PROP_ODOO_OP = "odoo-operation";
	
	public static final String EX_PROP_ODOO_PATIENT_ID = "patient-odoo-id";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_CREATE_IF_NOT_EXISTS = "createCustomerIfNotExist";
	
	public static final String EX_PROP_MODEL_NAME = "modelName";
	
	public static final String EX_PROP_EXTERNAL_ID = "externalId";
	
	public static final String EX_PROP_PRODUCT_ID = "odooProductId";
	
	public static final String EX_PROP_ODOO_USER_ID_KEY = "odooUserIdKey";
	
	public static final String EX_PROP_RPC_CLIENT_KEY = "xmlRpcClientKey";
	
	public static final String EX_PROP_RPC_CFG_KEY = "xmlRpcConfigKey";
	
	public static final String EX_PROP_QUOTE_ID = "quotation-id";
	
	public static final String EX_PROP_LINE_COUNT = "orderLineCount";
	
	public static final String EX_PROP_CREATE_QUOTE_IF_NOT_EXIST = "createQuoteIfNotExist";
	
	public static final String EX_PROP_LINE_CONCEPT = "orderLineConcept";
	
	public static final String EX_PROP_ORDER_LINE = "order-line";
	
	public static final String EX_PROP_SKIP_CUSTOMER_UPDATE = "skipCustomerUpdate";
	
	public static final String EX_PROP_QTY = "order-quantity";
	
	public static final String EX_PROP_ENC = "encounter";
	
	public static final String EX_PROP_QN_CONCEPT_UUID = "questionConceptUuid";
	
	public static final String ROUTE_ID_OBS_TO_ODOO_RESOURCE = "obs-to-odoo-resource";
	
	public static final String ROUTE_ID_SAVE_CALENDAR_EVENT = "save-calendar-event-in-odoo";
	
	public static final String ROUTE_ID_GET_RES_BY_NAME_FROM_ODOO = "get-resource-by-name-from-odoo";
	
	public static final String ROUTE_ID_GET_RES_BY_ID_FROM_ODOO = "get-resource-by-id-from-odoo";
	
	public static final String ROUTE_ID_GET_EXT_ID_MAP = "odoo-get-external-id-map";
	
	public static final String ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO = "get-resource-by-ext-id-from-odoo";
	
	public static final String ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC = "get-obs-by-concept-uuid-from-encounter";
	
	public static final String ROUTE_ID_OBS_TO_CUSTOMER = "obs-to-customer";
	
	public static final String ROUTE_ID_OBS_TO_RES_HANDLER = "obs-to-odoo-resource-handler";
	
	public static final String ROUTE_ID_GET_PARTNERS_BY_USERS = "get-partner-ids-by-user-ids";
	
	public static final String ROUTE_ID_OBS_TO_ADMISSION_EVENT = "obs-to-admission-calendar-event";
	
	public static final String ROUTE_ID_OBS_TO_DISCHARGE_EVENT = "obs-to-discharge-calendar-event";
	
	public static final String ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT = "get-obs-by-qn-on-form-in-visit";
	
	public static final String ROUTE_ID_OBS_TO_INVOICE_EVENT = "obs-to-invoicing-calendar-event";
	
	public static final String ROUTE_ID_GET_HSU_ID = "get-hsu-id";
	
	public static final String ROUTE_ID_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT = "get-most-recent-enc-by-form-in-visit";
	
	public static final String ROUTE_ID_FORM_VALIDATED_RULE = "is-obs-form-validated-rule";

    public static final String ROUTE_ID_IS_ENC_VALIDATED = "is-encounter-validated";
	
	public static final String URI_ODOO_AUTH = "direct:odoo-authentication";
	
	public static final String URI_ORDER_HANDLER = "direct:odoo-order-handler";
	
	public static final String URI_PATIENT_HANDLER = "direct:odoo-patient-handler";
	
	public static final String URI_PATIENT_ASSOCIATION_HANDLER = "direct:odoo-patient-association-handler";
	
	public static final String URI_GET_EXT_ID = "direct:" + ROUTE_ID_GET_EXT_ID_MAP;
	
	public static final String URI_GET_QUOTES = "direct:odoo-get-draft-quotations";
	
	public static final String URI_GET_LINE = "direct:odoo-get-order-line";
	
	public static final String URI_MANAGE_QUOTE = "direct:odoo-manage-quotation";
	
	public static final String URI_PROCESS_ORDER = "direct:odoo-process-order";
	
	public static final String URI_GET_ENTITY_BY_UUID = "direct:get-entity-by-uuid-from-openmrs";
	
	public static final String URI_MOCK_GET_ENTITY_BY_UUID = "mock:get-entity-by-uuid-from-openmrs";
	
	public static final String URI_OBS_TO_ODOO_RESOURCE = "direct:" + ROUTE_ID_OBS_TO_ODOO_RESOURCE;
	
	public static final String URI_PATIENT_UUID_TO_CUSTOMER = "direct:patient-uuid-to-odoo-customer";
	
	public static final String URI_OBS_TO_ORDER_LINE = "direct:odoo-obs-to-order-line";
	
	public static final String URI_VOIDED_OBS_PROCESSOR = "direct:voided-obs-to-order-line-processor";
	
	public static final String URI_NON_VOIDED_OBS_PROCESSOR = "direct:non-voided-obs-to-order-line-processor";
	
	public static final String URI_UUID_TO_CUSTOMER = "direct:patient-uuid-to-odoo-customer";
	
	public static final String URI_FORM_VALIDATED_RULE = "direct:" + ROUTE_ID_FORM_VALIDATED_RULE;
	
	public static final String URI_CONCEPT_LINE_PROCESSOR = "direct:concept-to-order-line-processor";
	
	public static final String URI_GET_CONCEPT_BY_MAPPING = "direct:get-concept-by-mapping-from-openmrs";
	
	public static final String URI_CONVERT_TO_CONCEPT_UUID = "direct:convert-to-concept-uuid-if-is-mapping";
	
	public static final String URI_PRP_HANDLER = "direct:odoo-prp-handler";
	
	public static final String URI_PERSON_HANDLER = "direct:odoo-person-handler";
	
	public static final String URI_CUSTOM_DATA = "direct:callback-get-custom-customer-data";
	
	public static final String URI_MANAGE_ORDER_LINE = "direct:odoo-manage-order-line";
	
	public static final String URI_OBS_CAPTURED_ON_FORM = "direct:obs-captured-on-form-rule";
	
	public static final String URI_SAVE_CALENDAR_EVENT = "direct:" + ROUTE_ID_SAVE_CALENDAR_EVENT;
	
	public static final String URI_GET_RES_BY_NAME_FROM_ODOO = "direct:" + ROUTE_ID_GET_RES_BY_NAME_FROM_ODOO;
	
	public static final String URI_GET_RES_BY_ID_FROM_ODOO = "direct:" + ROUTE_ID_GET_RES_BY_ID_FROM_ODOO;
	
	public static final String URI_GET_CONCEPT_BY_UUID_FROM_ENC = "direct:" + ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC;
	
	public static final String URI_OBS_TO_CUSTOMER = "direct:" + ROUTE_ID_OBS_TO_CUSTOMER;
	
	public static final String URI_OBS_TO_RES_HANDLER = "direct:" + ROUTE_ID_OBS_TO_RES_HANDLER;
	
	public static final String URI_GET_PARTNERS_BY_USERS = "direct:" + ROUTE_ID_GET_PARTNERS_BY_USERS;
	
	public static final String URI_OBS_TO_ADMISSION_EVENT = "direct:" + ROUTE_ID_OBS_TO_ADMISSION_EVENT;
	
	public static final String URI_OBS_TO_DISCHARGE_EVENT = "direct:" + ROUTE_ID_OBS_TO_DISCHARGE_EVENT;
	
	public static final String URI_OBS_TO_INVOICE_EVENT = "direct:" + ROUTE_ID_OBS_TO_INVOICE_EVENT;
	
	public static final String URI_GET_OBS_BY_QN_FORM_VISIT = "direct:" + ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT;
	
	public static final String URI_GET_RES_BY_EXT_ID_FROM_ODOO = "direct:" + ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO;
	
	public static final String URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT = "direct:"
	        + ROUTE_ID_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT;
	
	public static final String URI_GET_HSU_ID = "direct:" + ROUTE_ID_GET_HSU_ID;

	public static final String URI_IS_ENC_VALIDATED = "direct:"+ROUTE_ID_IS_ENC_VALIDATED;
	
	public static final String ODOO_BASE_URL = "http://test.odoo.test";
	
	public static final String LISTENER_URI = "direct:odoo-event-listener";
	
	public static final String ORDER_UUID_1 = "16170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	public static final String ORDER_UUID_2 = "26170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	public static final String PATIENT_UUID = "ba3b12d1-5c4f-415f-871b-b98a22137604";
	
	public static final String NAME_UUID = "0bca417f-fc68-40d7-ae6f-cffca7a5eff1";
	
	public static final String ADDRESS_UUID = "359022bf-4a58-4732-8cce-1e57f72f47b0";
	
	public static final String PATIENT_ID_UUID = "148bcfc1-360a-44b5-9539-e8718cd6e46f";
	
	public static final String ODOO_OP_CREATE = "create";
	
	public static final String ODOO_OP_WRITE = "write";
	
	public static final String ODOO_OP_UNLINK = "unlink";
	
	public static final String ODOO_OP_SEARCH_READ = "search_read";
	
	public static final String ODOO_RES_PRODUCT = "product.product";
	
	public static final String ODOO_USER_ID_KEY = "odoo-event-listener-odooUserId";
	
	public static final String RPC_CLIENT_KEY = "odoo-event-listener-xmlRpcClient";
	
	public static final String RPC_CONFIG_KEY = "odoo-event-listener-xmlRpcConfig";
	
	public static final String ODOO_RPC_METHOD = "execute_kw";
	
	public static final String APP_PROP_NAME_OBS_TO_ODOO_HANDLER = "obs.to.odoo.resource.handler.route";
	
	public static final String APP_PROP_NAME_ID_TYPE_UUID = "openmrs.identifier.type.uuid";
	
	public static final String APP_PROP_NAME_GRP_EXT_ID = "odoo.dormitory.notification.group.ext.id";
	
	public static final String APP_PROP_NAME_INVOICE_GRP_EXT_ID = "odoo.invoicing.notification.group.ext.id";
	
	public static final String APP_PROP_NAME_BASIC_SERVICE_PLAN_FORM_UUID = "basic.service.plan.form.uuid";
	
	public static final String APP_PROP_NAME_FINAL_ASSMT_FORM_UUID = "final.assessment.outcome.form.uuid";
	
	public static final String APP_PROP_NAME_FINAL_ASSMT_CONCEPT = "final.assmt.decision.question.concept";
	
	public static final String CONCEPT_UUID_VALIDATED = "1382a47a-3e63-11e9-b210-d663bd873d93";
	
	public static final String CONCEPT_UUID_YES = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	
	public static final String CONCEPT_UUID_PATIENT_TYPE = "5b2efd02-be26-4789-b5ec-7c5ceb428725";
	
	public static final String CONCEPT_UUID_INPATIENT = "a0b86497-cb21-46ad-9a9b-c99db8db9528";
	
	public static final String CONCEPT_UUID_ASSMT_DECISION = "675b4e02-1a96-4eda-af84-b6ebe0d715a4";
	
	public static final String BASIC_SERVICE_PLAN_FORM_UUID = "3b07b00c-1623-4380-af4a-4bb68244eff5";
	
	public static final String FINAL_ASSMT_FORM_UUID = "5fa318a9-eade-ea79-a96e-d91754135a5c";
	
	public static final String PARAM_MODEL_NAME = "modelName";
	
	public static final String MODEL_NAME_GROUPS = "res.groups";
	
}
