package org.egov.swcalculation.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.swcalculation.repository.SewerageCalculatorDao;
import org.egov.swcalculation.service.BillGeneratorService;
import org.egov.swcalculation.util.ResponseInfoFactory;
import org.egov.swcalculation.validator.BillGenerationValidator;
import org.egov.swcalculation.web.models.BillGenerationRequest;
import org.egov.swcalculation.web.models.BillGenerationSearchCriteria;
import org.egov.swcalculation.web.models.BillScheduler;
import org.egov.swcalculation.web.models.BillSchedulerResponse;
import org.egov.swcalculation.web.models.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@RestController
@RequestMapping("/seweragecharges")
public class BillGeneratorController {

	@Autowired
	private final ResponseInfoFactory responseInfoFactory;

	@Autowired
	private BillGeneratorService billGeneratorService;

	@Autowired
	private BillGenerationValidator billGenerationValidator;
	
	@Autowired
	private SewerageCalculatorDao sewerageCalculatorDao;
	
	@PostMapping("/scheduler/_create")
	public ResponseEntity<BillSchedulerResponse> billSchedulerCreate(
			@Valid @RequestBody BillGenerationRequest billGenerationReq) {
		BillSchedulerResponse response=new BillSchedulerResponse();
		List<BillScheduler> billDetails1 = new ArrayList<BillScheduler>();
		boolean isBatch=true;
		if(isBatch) {
			
			  List<String> listOfLocalities = new ArrayList<String>();
			  listOfLocalities.add("SC1"); 
			  listOfLocalities.add("SC2");
			  listOfLocalities.add("SC3");
		
	//	List<String> listOfLocalities = sewerageCalculatorDao.getLocalityList(billGenerationReq.getBillScheduler().getTenantId(),billGenerationReq.getBillScheduler().getLocality());
		
		for(String localityName : listOfLocalities) {
			
			//billGenerationValidator.validateBillingCycleDatesBatch(localityName,billGenerationReq.getBillScheduler().getTenantId(), billGenerationReq.getRequestInfo());
			billGenerationReq.getBillScheduler().setLocality(localityName);			
			billGenerationValidator.validateBillingCycleDates(billGenerationReq, billGenerationReq.getRequestInfo());
			billGenerationValidator.validateBillingCycleDates(billGenerationReq, billGenerationReq.getRequestInfo());
			List<BillScheduler> billDetails = billGeneratorService.saveBillGenerationDetails(billGenerationReq);
			billDetails1.addAll(billDetails);
				/*
				 * BillSchedulerResponse response =
				 * BillSchedulerResponse.builder().billSchedulers(billDetails) .responseInfo(
				 * responseInfoFactory.createResponseInfoFromRequestInfo(billGenerationReq.
				 * getRequestInfo(), true)) .build(); return new ResponseEntity<>(response,
				 * HttpStatus.CREATED);
				 */
		}
		}
		
		else {
			billGenerationValidator.validateBillingCycleDates(billGenerationReq, billGenerationReq.getRequestInfo());
			List<BillScheduler> billDetails = billGeneratorService.saveBillGenerationDetails(billGenerationReq);
			billDetails1.addAll(billDetails);
			/*
			 * BillSchedulerResponse response =
			 * BillSchedulerResponse.builder().billSchedulers(billDetails) .responseInfo(
			 * responseInfoFactory.createResponseInfoFromRequestInfo(billGenerationReq.
			 * getRequestInfo(), true)) .build(); return new ResponseEntity<>(response,
			 * HttpStatus.CREATED);
			 */
		}
		 response = BillSchedulerResponse.builder().billSchedulers(billDetails1)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(billGenerationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
}
