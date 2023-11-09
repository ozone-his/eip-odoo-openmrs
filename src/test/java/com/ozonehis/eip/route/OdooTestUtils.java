package com.ozonehis.eip.route;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

public class OdooTestUtils {
	
	public static Map createConcept(String uuid, String display) {
		Map res = new HashMap();
		res.put("uuid", uuid);
		res.put("display", display);
		return res;
	}
	
	public static Map createCodedObs(String conceptUuid, String valueUuid) {
		return createCodedObs(conceptUuid, valueUuid, null, null);
	}
	
	public static Map createCodedObs(String conceptUuid, String valueUuid, String conceptDisplay, String valueDisplay) {
		Map res = new HashMap();
		res.put("concept", createConcept(conceptUuid, conceptDisplay));
		res.put("value", createConcept(valueUuid, valueDisplay));
		return res;
	}
	
	public static <N extends Number> Map createNumericObs(String conceptUuid, N valueNumeric) {
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", conceptUuid));
		obsRes.put("value", valueNumeric);
		return obsRes;
	}
	
	public static Map createPatientId(String id, String identifierTypeUuid) {
		Map res = new HashMap();
		res.put("identifier", id);
		res.put("identifierType", singletonMap("uuid", identifierTypeUuid));
		return res;
	}
	
	public static Map createEnc(String encUuid, Map... obs) {
		Map res = new HashMap();
		res.put("uuid", encUuid);
		res.put("obs", asList(obs));
		return res;
	}
	
}
