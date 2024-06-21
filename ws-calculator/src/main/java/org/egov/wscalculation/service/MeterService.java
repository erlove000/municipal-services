package org.egov.wscalculation.service;

import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterConnectionRequests;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReadingList;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;


public interface MeterService {
	List<MeterReading> createMeterReading(MeterConnectionRequest meterConnectionRequest);

	List<MeterReadingList> createMeterReadings(MeterConnectionRequests meterConnectionlist);
	
	List<MeterReading> searchMeterReadings(MeterReadingSearchCriteria criteria, RequestInfo requestInfo);
}
