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

    public static final String EX_PROP_LINE_COUNT = "orderLineCount";
	
	public static final String URI_ODOO_AUTH = "direct:odoo-authentication";
	
	public static final String URI_ORDER_HANDLER = "direct:odoo-order-handler";
	
	public static final String URI_PATIENT_HANDLER = "direct:odoo-patient-handler";
	
	public static final String URI_PATIENT_ASSOCIATION_HANDLER = "direct:odoo-patient-association-handler";
	
	public static final String URI_GET_EXT_ID = "direct:odoo-get-external-id-map";
	
	public static final String URI_GET_QUOTES = "direct:odoo-get-draft-quotations";
	
	public static final String URI_GET_LINE = "direct:odoo-get-order-line";
	
	public static final String URI_MANAGE_QUOTE = "direct:odoo-manage-quotation";
	
	public static final String URI_PROCESS_ORDER = "direct:odoo-process-order";
	
	public static final String URI_FETCH_RESOURCE = "direct:odoo-fetch-resource";
	
	public static final String URI_MOCK_FETCH_RESOURCE = "mock:odoo-fetch-resource";
	
	public static final String URI_OBS_HANDLER = "direct:odoo-obs-to-customer";
	
	public static final String URI_PATIENT_UUID_TO_CUSTOMER = "direct:patient-uuid-to-odoo-customer";
	
	public static final String URI_OBS_TO_ORDER_LINE = "direct:odoo-obs-to-order-line";
	
	public static final String URI_VOIDED_OBS_PROCESSOR = "direct:voided-obs-to-order-line-processor";
	
	public static final String URI_NON_VOIDED_OBS_PROCESSOR = "direct:non-voided-obs-to-order-line-processor";
	
	public static final String URI_UUID_TO_CUSTOMER = "direct:patient-uuid-to-odoo-customer";
	
	public static final String URI_ENC_VALIDATED_RULE = "direct:is-obs-encounter-validated-rule";
	
	public static final String URI_CONCEPT_LINE_PROCESSOR = "direct:concept-to-order-line-processor";
	
	public static final String URI_PRP_HANDLER = "direct:odoo-prp-handler";
	
	public static final String URI_PERSON_HANDLER = "direct:odoo-person-handler";
	
	public static final String URI_CUSTOM_DATA = "direct:odoo-callback-get-custom-customer-data";
	
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
	
	public static final String ODOO_RES_PRODUCT = "product.product";
	
	public static final String ODOO_USER_ID_KEY = "odoo-event-listener-odooUserId";
	
	public static final String RPC_CLIENT_KEY = "odoo-event-listener-xmlRpcClient";
	
	public static final String RPC_CONFIG_KEY = "odoo-event-listener-xmlRpcConfig";
	
}
