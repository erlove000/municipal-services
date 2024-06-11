package org.egov.wscalculation.service;

import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.web.models.Calculation;
import org.egov.wscalculation.web.models.CalculationReq;
// import org.egov.wscalculation.web.models.SingleDemand;
public interface WSCalculationService {

	List<Calculation> getCalculation(CalculationReq calculationReq);

	void jobScheduler();

	void generateDemandBasedOnTimePeriod(RequestInfo requestInfo);
	// void generateSingleDemand(SingleDemand singledemand);
	void generateBillBasedLocality(RequestInfo requestInfo);
}
