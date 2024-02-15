package org.egov.swcalculation.repository;

import java.util.List;

import org.egov.swcalculation.web.models.SewerageDetails;

public interface SewerageCalculatorDao {

	List<String> getTenantId();
	
	List<SewerageDetails> getConnectionsNoList(String tenantId, String connectionType, Long taxPeriodFrom, Long taxPeriodTo, String cone );
	
	List<String> getConnectionsNoByLocality(String tenantId, String connectionType, String locality);
	
	Long searchLastDemandGenFromDate(String consumerCode, String tenantId);
	
	Boolean isConnectionDemandAvailableForBillingCycle(String tenantId, Long taxPeriodFrom, Long taxPeriodTo, String consumerCode);

	List<String> getLocalityList(String tenantId, String locality);

	void updateBillStatus(List<String> consumerCodes, String businessservice, String status);



}
