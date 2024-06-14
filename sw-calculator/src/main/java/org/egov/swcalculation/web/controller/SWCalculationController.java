package org.egov.swcalculation.web.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

import org.egov.swcalculation.web.models.AdhocTaxReq;
import org.egov.swcalculation.web.models.Calculation;
import org.egov.swcalculation.web.models.CalculationReq;
import org.egov.swcalculation.web.models.CalculationRes;
import org.egov.swcalculation.web.models.DemandResponse;
import org.egov.swcalculation.web.models.GetBillCriteria;
import org.egov.swcalculation.web.models.RequestInfoWrapper;
import org.egov.swcalculation.web.models.SingleDemand;
import org.egov.swcalculation.service.DemandService;
import org.egov.swcalculation.service.SWCalculationService;
import org.egov.swcalculation.service.SWCalculationServiceImpl;
import org.egov.swcalculation.util.ResponseInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@RestController
@RequestMapping("/sewerageCalculator")
public class SWCalculationController {
	
	@Autowired
	private SWCalculationService sWCalculationService;
	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private ResponseInfoFactory responseInfoFactory;
	
	@Autowired
	private SWCalculationServiceImpl sWCalculationServiceImpl;
	
	@PostMapping("/_calculate")
	public ResponseEntity<CalculationRes> calculate(@RequestBody @Valid CalculationReq calculationReq) {
		List<Calculation> calculations = sWCalculationService.getCalculation(calculationReq);
		CalculationRes response = CalculationRes.builder().calculation(calculations)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(calculationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/_estimate")
	public ResponseEntity<CalculationRes> getTaxEstimation(@RequestBody @Valid CalculationReq calculationReq) {
		List<Calculation> calculations = sWCalculationService.getEstimation(calculationReq);
		CalculationRes response = CalculationRes.builder().calculation(calculations)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(calculationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/_updateDemand")
	public ResponseEntity<DemandResponse> updateDemands(@RequestBody @Valid RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute @Valid GetBillCriteria getBillCriteria) {
		return new ResponseEntity<>(demandService.updateDemands(getBillCriteria, requestInfoWrapper), HttpStatus.OK);
	}
	
	@PostMapping("/_jobscheduler")
	public void jobscheduler(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
		sWCalculationService.generateDemandBasedOnTimePeriod(requestInfoWrapper.getRequestInfo());
	}

	@PostMapping("/_singledemand")
	
// //	 public ResponseEntity<String> singledemandgen(@Valid @RequestBody SingleDemand singledemand) {
// 	public void _singledemand(@Valid @RequestBody SingleDemand singledemand) {
// //		log.info("singledemandgen::");
		 
// 		     sWCalculationService.generateSingleDemand(singledemand);
// //	            return ResponseEntity.status(HttpStatus.OK).body("Demand generated successfully");
// 	        } 

	public ResponseEntity<Map<String, Object>> singledemandgen(@Valid @RequestBody SingleDemand singledemand) {
	    Map<String, Object> response = new HashMap<>(); 

		 try {
	     String singleresponse=   sWCalculationService.generateSingleDemand(singledemand);
	     if (singleresponse==null) {
	    	 response.put("status", "Failed");String Message="Unable to Generate Demand for Connection No: ".concat(singledemand.getConsumercode());
	 	    response.put("message", Message);
	 	   return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	     else {response.put("status", "Success"); String Message="Single demand generated successfully for Connection No: ".concat(singledemand.getConsumercode());
		    response.put("message",Message);
//	            log.info("singledemandgen:: Demand generated successfully for: {}", singledemand);
	            return new ResponseEntity<>(response,HttpStatus.OK );
	     }
	        } catch (Exception e) {
	        	response.put("status", "failed");
//	            log.error("singledemandgen:: Error generating demand for: {}", singledemand, e);
	            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
     }
	}
	
	@PostMapping("/_jobbillscheduler")
	public void jobbillscheduler(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper) {
		sWCalculationService.generateBillBasedLocality(requestInfoWrapper.getRequestInfo());
	}

	@PostMapping("/_applyAdhocTax")
	public ResponseEntity<CalculationRes> applyAdhocTax(@Valid @RequestBody AdhocTaxReq adhocTaxReq) {
		List<Calculation> calculations = sWCalculationServiceImpl.applyAdhocTax(adhocTaxReq);
		CalculationRes response = CalculationRes.builder().calculation(calculations)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(adhocTaxReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
}
