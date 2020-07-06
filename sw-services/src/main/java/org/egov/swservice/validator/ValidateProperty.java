package org.egov.swservice.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.swservice.model.Property;
import org.egov.swservice.model.SewerageConnection;
import org.egov.swservice.model.SewerageConnectionRequest;
import org.egov.swservice.model.Status;
import org.egov.swservice.util.SewerageServicesUtil;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ValidateProperty {

	@Autowired
	private SewerageServicesUtil sewerageServiceUtil;

	/**
	 *
	 * @param property
	 */
	public void validatePropertyFields(Property property) {
		if (StringUtils.isEmpty(property.getTenantId())) {
			throw new CustomException("INVALID PROPERTY", "SewerageConnection cannot be updated without tenantId");
		}
		if (org.springframework.util.StringUtils.isEmpty(property.getStatus()) || (!property.getStatus().equals(Status.ACTIVE))) {
			throw new CustomException("INVALID_PROPERTY_STATUS", " Please enter a valid property ID");
		}
	}

	
	public Property getOrValidateProperty(SewerageConnectionRequest sewerageConnectionRequest) {
		Optional<Property> propertyList = sewerageServiceUtil.propertySearch(sewerageConnectionRequest).stream()
				.findFirst();
		if (!propertyList.isPresent()) {
			throw new CustomException("INVALID WATER CONNECTION PROPERTY",
					"Water connection cannot be enriched without property");
		} 
		Property property = propertyList.get();
		if (StringUtils.isEmpty(property.getUsageCategory())) {
			throw new CustomException("INVALID WATER CONNECTION PROPERTY USAGE TYPE",
					"Water connection cannot be enriched without property usage type");
		}

		return property;
	}

	public void validatePropertyForConnection(List<SewerageConnection> sewerageConnectionList) {
		sewerageConnectionList.forEach(sewerageConnection -> {
			if (StringUtils.isEmpty(sewerageConnection.getPropertyId())) {
				StringBuilder builder = new StringBuilder();
				builder.append("PROPERTY ID NOT FOUND FOR ")
						.append(sewerageConnection.getConnectionNo() == null ? sewerageConnection.getApplicationNo()
								: sewerageConnection.getConnectionNo());
				log.error("", builder.toString());
			}
		});
	}
}
