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
		List<BillScheduler> billDetails = new ArrayList<BillScheduler>();
		//boolean is
		String isBatch=billGenerationReq.getBillScheduler().getIsBatch();
        System.out.println("isBatch value"+isBatch);
        boolean batchBilling=false;
		if(isBatch.equals("true")) {
			batchBilling = true;
		}
       	 if(batchBilling) {		
		List<String> listOfLocalities = sewerageCalculatorDao.getLocalityList(billGenerationReq.getBillScheduler().getTenantId(),billGenerationReq.getBillScheduler().getLocality());
		for(String localityName : listOfLocalities) {		
			billGenerationReq.getBillScheduler().setLocality(localityName);			
			boolean localityStatus = billGenerationValidator.checkBillingCycleDates(billGenerationReq, billGenerationReq.getRequestInfo());
			if(!localityStatus) {
			billDetails = billGeneratorService.saveBillGenerationDetails(billGenerationReq);
			}
			billDetails1.addAll(billDetails);
		}
		}			
		else {
			billGenerationValidator.validateBillingCycleDates(billGenerationReq, billGenerationReq.getRequestInfo());
			billDetails = billGeneratorService.saveBillGenerationDetails(billGenerationReq);
			billDetails1.addAll(billDetails);
		}
		 response = BillSchedulerResponse.builder().billSchedulers(billDetails1)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(billGenerationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@PostMapping("/scheduler/_search")
	public ResponseEntity<BillSchedulerResponse> billSchedulerSearch(
			@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute BillGenerationSearchCriteria criteria) {
		List<BillScheduler> billSchedulers = billGeneratorService.getBillGenerationDetails(criteria);
		BillSchedulerResponse response = BillSchedulerResponse.builder().billSchedulers(billSchedulers).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
