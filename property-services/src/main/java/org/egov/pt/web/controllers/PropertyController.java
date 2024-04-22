
package org.egov.pt.web.controllers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.oldProperty.OldPropertyCriteria;
import org.egov.pt.service.MigrationService;
import org.egov.pt.service.PropertyService;
import org.egov.pt.util.NotificationUtil;
import org.egov.pt.util.ResponseInfoFactory;
import org.egov.pt.validator.PropertyValidator;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.pt.web.contracts.PropertyResponse;
import org.egov.pt.web.contracts.RequestInfoWrapper;
import org.egov.pt.web.contracts.SMSRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Controller
@Slf4j
@RequestMapping("/property")

public class PropertyController {

	@Autowired
	private NotificationUtil notifUtil;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ResponseInfoFactory responseInfoFactory;

    @Autowired
    private MigrationService migrationService;

    @Autowired
    private PropertyValidator propertyValidator;

    @PostMapping("/_create")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest propertyRequest) {
        Property property = propertyService.createProperty(propertyRequest);
        ResponseInfo resInfo = responseInfoFactory.createResponseInfoFromRequestInfo(propertyRequest.getRequestInfo(), true);
        PropertyResponse response = PropertyResponse.builder()
                .properties(Arrays.asList(property))
                .responseInfo(resInfo)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @PostMapping("/_update")
    public ResponseEntity<PropertyResponse> update(@Valid @RequestBody PropertyRequest propertyRequest) {
        Property property = propertyService.updateProperty(propertyRequest);
        ResponseInfo resInfo = responseInfoFactory.createResponseInfoFromRequestInfo(propertyRequest.getRequestInfo(), true);
        PropertyResponse response = PropertyResponse.builder()
                .properties(Arrays.asList(property))
                .responseInfo(resInfo)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/_search")
    public ResponseEntity<PropertyResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
                                                   @Valid @ModelAttribute PropertyCriteria propertyCriteria) {
		log.info("Update your number: "+propertyCriteria.getMobileNumber());
        propertyValidator.validatePropertyCriteria(propertyCriteria, requestInfoWrapper.getRequestInfo());
        List<Property> properties = propertyService.searchProperty(propertyCriteria,requestInfoWrapper.getRequestInfo());
        PropertyResponse response = PropertyResponse.builder().properties(properties).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
                .build();
	log.info("property found is==== "+response.getProperties().size());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
   
    @PostMapping("/_migration")
    public ResponseEntity<?> propertyMigration(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
                                               @Valid @ModelAttribute OldPropertyCriteria propertyCriteria) {
        long startTime = System.nanoTime();
        Map<String, String> resultMap = null;
        Map<String, String> errorMap = new HashMap<>();

        resultMap = migrationService.initiateProcess(requestInfoWrapper,propertyCriteria,errorMap);

        long endtime = System.nanoTime();
        long elapsetime = endtime - startTime;
        System.out.println("Elapsed time--->"+elapsetime);

        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

 
    
    @RequestMapping(value = "/_plainsearch", method = RequestMethod.POST)
    public ResponseEntity<PropertyResponse> plainsearch(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
                                                        @Valid @ModelAttribute PropertyCriteria propertyCriteria) {
        List<Property> properties = propertyService.searchPropertyPlainSearch(propertyCriteria, requestInfoWrapper.getRequestInfo());
        PropertyResponse response = PropertyResponse.builder().properties(properties).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    
    @RequestMapping(value = "/_sendOpenSMS", method = RequestMethod.POST)
    public ResponseEntity<Integer> sendOpenSMS(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper)
    {
    	String msg="Dear OpenSMS, Status for your application no.OpenSMSApp for property OpenPid to OpenCreated property has been changed to OpenActivate. You can track your application on the link given below-https://mseva.lgpunjab.gov.in/employee/user/login Thank you|1301157492438182299|1407162859196626520";
    	Map<String, String> mobileNumberToOwner = new HashMap<>();
    	mobileNumberToOwner.put("9417630724", "gurpreet");
    	List<SMSRequest> smsRequests = notifUtil.createSMSRequest(msg, mobileNumberToOwner);
		notifUtil.sendSMS(smsRequests);
		
    	return new ResponseEntity<>(1,HttpStatus.OK);
    }
    
//	@RequestMapping(value = "/_cancel", method = RequestMethod.POST)
//	public ResponseEntity<PropertyResponse> cancel(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
//												   @Valid @ModelAttribute PropertyCancelCriteria propertyCancelCriteria) {
//
//		List<Property> properties = propertyService.cancelProperty(propertyCancelCriteria,requestInfoWrapper.getRequestInfo());
//		PropertyResponse response = PropertyResponse.builder().properties(properties).responseInfo(
//				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
//				.build();
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}

}
