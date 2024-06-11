package org.egov.swcalculation.service;

import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.swcalculation.web.models.Calculation;
import org.egov.swcalculation.web.models.CalculationReq;
import org.egov.swcalculation.web.models.SingleDemand;


public interface SWCalculationService {
	
	List<Calculation> getCalculation(CalculationReq request);
	
	void generateDemandBasedOnTimePeriod(RequestInfo requestInfo);

	void generateSingleDemand(SingleDemand singledemand);

	List<Calculation> getEstimation(CalculationReq request);
	
	void generateBillBasedLocality(RequestInfo requestInfo);

}
