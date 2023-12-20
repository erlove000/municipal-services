package org.egov.pgr.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.pgr.contract.RequestInfoWrapper;
import org.egov.pgr.contract.SMSRequest;
import org.egov.pgr.contract.ServiceReqSearchCriteria;
import org.egov.pgr.contract.ServiceRequest;
import org.egov.pgr.contract.ServiceResponse;
import org.egov.pgr.producer.PGRProducer;
import org.egov.pgr.service.GrievanceService;
import org.egov.pgr.utils.DGRApi;
import org.egov.pgr.validator.PGRRequestValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping(value = "/v1/requests/")
public class ServiceController {

	@Autowired
	private GrievanceService service;
	
    @Autowired
    private PGRProducer pGRProducer;

	@Autowired
	private PGRRequestValidator pgrRequestValidator;
	
	@Autowired
	private RestTemplate restTemplate;
	
    @Value("${kafka.topics.notification.sms}")
    private String smsNotifTopic;
    
	@Value("${egov.mdms.host}")
	private String mdmsHost;
	
	/**Dharamshala0
	 * enpoint to create service requests
	 * 
	 * @param ServiceReqRequest
	 * @author kaviyarasan1993
	 */
	@PostMapping("_create")
	@ResponseBody
	private ResponseEntity<?> create(@RequestBody @Valid ServiceRequest serviceRequest) {
		pgrRequestValidator.validateCreate(serviceRequest);
		ServiceResponse response = service.create(serviceRequest);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	/**
	 * enpoint to update service requests
	 * 
	 * @param ServiceReqRequest
	 * @author kaviyarasan1993
	 */
	@PostMapping("_update")
	@ResponseBody
	private ResponseEntity<?> update(@RequestBody @Valid ServiceRequest serviceRequest) {
		pgrRequestValidator.validateUpdate(serviceRequest);
		ServiceResponse response = service.update(serviceRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	

	/**
	 * Controller endpoint to fetch service requests
	 * 
	 * @param requestInfoWrapper
	 * @param serviceReqSearchCriteria
	 * @return ResponseEntity<?>
	 * @author vishal
	 */
	@PostMapping("_search")
	@ResponseBody
	private ResponseEntity<?> search(@RequestBody @Valid RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute @Valid ServiceReqSearchCriteria serviceReqSearchCriteria) {
		pgrRequestValidator.validateSearch(serviceReqSearchCriteria, requestInfoWrapper.getRequestInfo());
		Object serviceReqResponse = service.getServiceRequestDetails(requestInfoWrapper.getRequestInfo(),
				serviceReqSearchCriteria);
		return new ResponseEntity<>(serviceReqResponse, HttpStatus.OK);
	}
	
	/**
	 * Controller endpoint to fetch service requests irrespective of role
	 * 
	 * @param requestInfoWrapper
	 * @param serviceReqSearchCriteria
	 * @return ResponseEntity<?>
	 * @author vishal
	 */
	@PostMapping("_plainsearch")
	@ResponseBody
	private ResponseEntity<?> plainsearch(@RequestBody @Valid RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute @Valid ServiceReqSearchCriteria serviceReqSearchCriteria) {
		Object serviceReqResponse = service.getServiceRequestDetailsForPlainSearch(requestInfoWrapper.getRequestInfo(),
				serviceReqSearchCriteria);
		return new ResponseEntity<>(serviceReqResponse, HttpStatus.OK);
	}

	 /**
	 * Controller to fetch count of service requests based on a given criteria
	 * 
	 * @param requestInfoWrapper
	 * @param serviceReqSearchCriteria
	 * @return ResponseEntity<?>
	 * @author vishal
	 */
	@PostMapping("_count")
	@ResponseBody
	private ResponseEntity<?> count(@RequestBody @Valid RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute @Valid ServiceReqSearchCriteria serviceReqSearchCriteria) {
		pgrRequestValidator.validateSearch(serviceReqSearchCriteria, requestInfoWrapper.getRequestInfo());
		Object countResponse = service.getCount(requestInfoWrapper.getRequestInfo(), serviceReqSearchCriteria);
		return new ResponseEntity<>(countResponse, HttpStatus.OK);
	}
	
	
	@PostMapping("_apicall")
	@ResponseBody
	private ResponseEntity<?> callApi(@RequestParam String complaintId) {
		String responce = new DGRApi().apiCalling(complaintId);
		return new ResponseEntity<>(responce, HttpStatus.OK);
	}
	
	@PostMapping("_countOpen")
	@ResponseBody
	private ResponseEntity<?> countOpen(@RequestBody @Valid RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute @Valid ServiceReqSearchCriteria serviceReqSearchCriteria) {
		pgrRequestValidator.validateSearch(serviceReqSearchCriteria, requestInfoWrapper.getRequestInfo());
		//Object countResponse = service.getCountOpen(requestInfoWrapper.getRequestInfo(), serviceReqSearchCriteria);

		
        /*Integer count = pgrService.countOpen(requestInfoWrapper.getRequestInfo(), criteria);
    	
    	// get count of unassigned applications for last 24 hours for each tenant as json
		pgrRequestValidator.validateSearch(serviceReqSearchCriteria, requestInfoWrapper.getRequestInfo());
		String resultjson = (String)service.getCountOpen(requestInfoWrapper.getRequestInfo(), serviceReqSearchCriteria);
		*/
    	JSONArray arr=service.getCountOpen(requestInfoWrapper.getRequestInfo(), serviceReqSearchCriteria);
    	JSONObject obj;
    	String uri=mdmsHost+"/egov-mdms-service/v1/_get?moduleName=tenant&masterName=tenants&tenantId=pb";
        String tenantsJson = restTemplate.postForObject(uri, requestInfoWrapper.getRequestInfo(), String.class);
    	JSONObject jsonObj=(new JSONObject(tenantsJson)).getJSONObject("MdmsRes").getJSONObject("tenant");//.get("data").toString();//=obj.getJSONArray("data");
		JSONArray tenantsArray = jsonObj.getJSONArray("tenants");
		String phone;
        
    	for(int i=0;i<arr.length();i++)
    	{
    		obj=(JSONObject)arr.get(i);
    		phone=fetchPGRMasterContact(tenantsArray, obj.getString("tenantid")); //PGRMaster contact is contact number of tenant where sms should be fired for unassigned complaints
    		System.out.println(obj.getString("tenantid"));
    		System.out.println(obj.getInt("count"));
    		
    		 List<SMSRequest> smsRequestsTobeSent = new ArrayList<>();
    		//String msg="Dear "+obj.getString("tenantid")+", Your Trade License Application for "+obj.getInt("count")+" is sent for field verification. You may be contacted by the Field Inspector for further verification.  Thank You|1301157492438182299|1407160811702191801";
    		String duration="24 hours";
    		String msg="Alert, "+obj.getString("tenantid")+" ULB have "+obj.getInt("count")+" unassigned complaints for more than "+duration+" in mseva PGRS.PMIDC|1301157492438182299|1407170254756596436";
    		System.out.println("sending message "+msg+" to "+phone);
    		
    		smsRequestsTobeSent.add(SMSRequest.builder().mobileNumber(phone).message(msg).build());
    		for (SMSRequest smsRequest : smsRequestsTobeSent) {
                pGRProducer.push(smsNotifTopic, smsRequest);
                System.out.println("sent sms to "+phone+" as: "+msg);
          /*  smsRequests = new NotificationService().enrichSmsRequest("9417630724",msg);
            if (!CollectionUtils.isEmpty(smsRequests)) {
                notificationUtil.sendSMS(smsRequests);
            }*/
    	}
    	}
    	//int count=0;
       // ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true);
        //Object  response = CountResponse.builder().responseInfo(responseInfo).count(count).build();
		return new ResponseEntity<>(arr.toString(), HttpStatus.OK);
       // return new ResponseEntity<>(resultjson, HttpStatus.OK);


//		return new ResponseEntity<>(countResponse, HttpStatus.OK);
	

}
	
	
	private String fetchPGRMasterContact(JSONArray tenants,String tenantCode) //PGRMaster contact is contact number of tenant where sms should be fired for unassigned complaints
	{
		JSONObject obj;
		try
		{
		for(int i=0;i<tenants.length();i++)
		  {
		   	obj=tenants.getJSONObject(i);
		   	if(obj.getString("code").contentEquals(tenantCode))
		   		return obj.getString("PGRMasterContact");
		  }
		}
		catch (Exception ex)
		{
			System.out.println("not able to find PGRMasterContact attribute for tenant "+tenantCode);
		}
		return "12345"; // default contact Number
	}
}
