package org.egov.waterconnection.repository.builder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.service.UserService;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.web.models.Property;
import org.egov.waterconnection.web.models.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static org.egov.waterconnection.constants.WCConstants.SEARCH_TYPE_CONNECTION;

@Component
@Slf4j
public class WsQueryBuilder {

	@Autowired
	private WaterServicesUtil waterServicesUtil;

	@Autowired
	private WSConfiguration config;
	
	@Autowired
	private UserService userService;

	private static final String INNER_JOIN_STRING = "INNER JOIN";
    private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";
//	private static final String Offset_Limit_String = "OFFSET ? LIMIT ?";
    
    private static String holderSelectValues = "connectionholder.tenantid as holdertenantid, connectionholder.connectionid as holderapplicationId, userid, connectionholder.status as holderstatus, isprimaryholder, connectionholdertype, holdershippercentage, connectionholder.relationship as holderrelationship, connectionholder.createdby as holdercreatedby, connectionholder.createdtime as holdercreatedtime, connectionholder.lastmodifiedby as holderlastmodifiedby, connectionholder.lastmodifiedtime as holderlastmodifiedtime";

    
	private static final String WATER_SEARCH_QUERY = "SELECT conn.*, wc.*, document.*, plumber.*, wc.connectionCategory, wc.connectionType, wc.waterSource,"
			+ " wc.meterId, wc.meterInstallationDate, wc.pipeSize, wc.noOfTaps, wc.proposedPipeSize, wc.proposedTaps, wc.connection_id as connection_Id, wc.connectionExecutionDate, wc.initialmeterreading, wc.appCreatedDate,"
			+ " wc.detailsprovidedby, wc.estimationfileStoreId , wc.sanctionfileStoreId , wc.estimationLetterDate,"
			+ " conn.id as conn_id, conn.tenantid, conn.applicationNo, conn.applicationStatus, conn.status, conn.connectionNo, conn.oldConnectionNo,conn.isoldapplication, conn.property_id, conn.roadcuttingarea,"
			+ " conn.action, conn.adhocpenalty, conn.adhocrebate, conn.adhocpenaltyreason, conn.applicationType, conn.dateEffectiveFrom,"
			+ " conn.adhocpenaltycomment, conn.adhocrebatereason, conn.adhocrebatecomment, conn.createdBy as ws_createdBy, conn.lastModifiedBy as ws_lastModifiedBy,"
			+ " conn.createdTime as ws_createdTime, conn.lastModifiedTime as ws_lastModifiedTime,conn.additionaldetails, "
			+ " conn.locality, conn.isoldapplication, conn.roadtype, document.id as doc_Id, document.documenttype, document.filestoreid, document.active as doc_active, plumber.id as plumber_id,"
			+ " plumber.name as plumber_name, plumber.licenseno, roadcuttingInfo.id as roadcutting_id, roadcuttingInfo.roadtype as roadcutting_roadtype, roadcuttingInfo.roadcuttingarea as roadcutting_roadcuttingarea, roadcuttingInfo.roadcuttingarea as roadcutting_roadcuttingarea,"
			+ " roadcuttingInfo.active as roadcutting_active, plumber.mobilenumber as plumber_mobileNumber, plumber.gender as plumber_gender, plumber.fatherorhusbandname, plumber.correspondenceaddress,"
			+ " plumber.relationship, " + holderSelectValues
			+ " FROM eg_ws_connection conn "
			+  INNER_JOIN_STRING 
			+" eg_ws_service wc ON wc.connection_id = conn.id"
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_applicationdocument document ON document.wsid = conn.id" 
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_plumberinfo plumber ON plumber.wsid = conn.id"
		    +  LEFT_OUTER_JOIN_STRING
		    + "eg_ws_connectionholder connectionholder ON connectionholder.connectionid = conn.id"
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_roadcuttinginfo roadcuttingInfo ON roadcuttingInfo.wsid = conn.id" ;

	private static final String PAGINATION_WRAPPER = "SELECT * FROM " +
            "(SELECT *, DENSE_RANK() OVER (ORDER BY conn_id) offset_ FROM " +
            "({})" +
            " result) result_offset " +
            "WHERE offset_ > ? AND offset_ <= ?";
	
	private static final String ORDER_BY_CLAUSE= " ORDER BY wc.appCreatedDate DESC";
	
	
	public static final String UPDATE_DISCONNECT_STATUS="update eg_ws_connection set status=? where id=?";
	
	/**
	 * 
	 * @param criteria
	 *            The WaterCriteria
	 * @param preparedStatement
	 *            The Array Of Object
	 * @param requestInfo
	 *            The Request Info
	 * @return query as a string
	 */
	public String getSearchQueryString(SearchCriteria criteria, List<Object> preparedStatement,
			RequestInfo requestInfo) {
		log.info("Search Criteria Passed :::"+ criteria);
		if (criteria.isEmpty())
				return null;
		StringBuilder query = new StringBuilder(WATER_SEARCH_QUERY);
		log.info("Search Query Loaded :::"+ query);
		boolean propertyIdsPresent = false;
		
		String propertyIdQuery = " (conn.property_id in (";

		if (!StringUtils.isEmpty(criteria.getMobileNumber()) || !StringUtils.isEmpty(criteria.getPropertyId()) || !StringUtils.isEmpty(criteria.getName() )) {
			Set<String> propertyIds = new HashSet<>();
			List<Property> propertyList = waterServicesUtil.propertySearchOnCriteria(criteria, requestInfo);
			propertyList.forEach(property -> propertyIds.add(property.getPropertyId()));
			if (!propertyIds.isEmpty()) {
				addClauseIfRequired(preparedStatement, query);
				query.append(propertyIdQuery).append(createQuery(propertyIds)).append(" )");
				addToPreparedStatement(preparedStatement, propertyIds);
				propertyIdsPresent = true;
			}
		}
		if(!StringUtils.isEmpty(criteria.getMobileNumber())) {
			Set<String> uuids = userService.getUUIDForUsers(criteria.getMobileNumber(), criteria.getTenantId(), requestInfo,criteria.getName());
			boolean userIdsPresent = false;
			if (!CollectionUtils.isEmpty(uuids)) {
				addORClauseIfRequired(preparedStatement, query);
				if(!propertyIdsPresent)
					query.append("(");
				query.append(" connectionholder.userid in (").append(createQuery(uuids)).append(" ))");
				addToPreparedStatement(preparedStatement, uuids);
				userIdsPresent = true;
			}
			if(propertyIdsPresent && !userIdsPresent){
				query.append(")");
			}
		}

			if(!StringUtils.isEmpty(criteria.getName())) {
			Set<String> uuids = userService.getUUIDForUsers(criteria.getMobileNumber(), criteria.getTenantId(), requestInfo,criteria.getName());
			boolean userIdsPresent = false;
			if (!CollectionUtils.isEmpty(uuids)) {
				addORClauseIfRequired(preparedStatement, query);
				if(!propertyIdsPresent)
					query.append("(");
				query.append(" connectionholder.userid in (").append(createQuery(uuids)).append(" ))");
				addToPreparedStatement(preparedStatement, uuids);
				userIdsPresent = true;
			}
			if(propertyIdsPresent && !userIdsPresent){
				query.append(")");
			}
		}


		
		if (!StringUtils.isEmpty(criteria.getTenantId())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.tenantid = ? ");
			preparedStatement.add(criteria.getTenantId());
		}
		if (!StringUtils.isEmpty(criteria.getPropertyId()) && StringUtils.isEmpty(criteria.getMobileNumber())) {
			if(propertyIdsPresent)
				query.append(")");
			else{
				addClauseIfRequired(preparedStatement, query);
				query.append(" conn.property_id = ? ");
				preparedStatement.add(criteria.getPropertyId());
			}
		}
		if (!CollectionUtils.isEmpty(criteria.getIds())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.id in (").append(createQuery(criteria.getIds())).append(" )");
			addToPreparedStatement(preparedStatement, criteria.getIds());
		}
		if (!StringUtils.isEmpty(criteria.getOldConnectionNumber())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.oldconnectionno = ? ");
			preparedStatement.add(criteria.getOldConnectionNumber());
		}

		if (!StringUtils.isEmpty(criteria.getConnectionNumber())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.connectionno = ? ");
			preparedStatement.add(criteria.getConnectionNumber());
		}
		if (!StringUtils.isEmpty(criteria.getStatus())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.status = ? ");
			preparedStatement.add(criteria.getStatus());
		}
		if (!StringUtils.isEmpty(criteria.getApplicationNumber())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.applicationno = ? ");
			preparedStatement.add(criteria.getApplicationNumber());
		}
		if (!StringUtils.isEmpty(criteria.getApplicationStatus())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.applicationStatus = ? ");
			preparedStatement.add(criteria.getApplicationStatus());
		}
		if (criteria.getFromDate() != null) {
			addClauseIfRequired(preparedStatement, query);
			query.append("  wc.appCreatedDate >= ? ");
			preparedStatement.add(criteria.getFromDate());
		}
		if (criteria.getToDate() != null) {
			addClauseIfRequired(preparedStatement, query);
			query.append("  wc.appCreatedDate <= ? ");
			preparedStatement.add(criteria.getToDate());
		}
		if(!StringUtils.isEmpty(criteria.getApplicationType())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.applicationType = ? ");
			preparedStatement.add(criteria.getApplicationType());
		}
		if(!StringUtils.isEmpty(criteria.getSearchType())
				&& criteria.getSearchType().equalsIgnoreCase(SEARCH_TYPE_CONNECTION)){
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.isoldapplication = ? ");
			preparedStatement.add(Boolean.FALSE);
		}
		if (!StringUtils.isEmpty(criteria.getLocality())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.locality = ? ");
			preparedStatement.add(criteria.getLocality());
		}
		if(criteria.getConnectionType().equalsIgnoreCase("METERED")) {
			if (!StringUtils.isEmpty(criteria.getConnectionType())) {
				addClauseIfRequired(preparedStatement, query);
				query.append(" wc.connectiontype = ? ");
				preparedStatement.add(criteria.getConnectionType());
			}
		}
	
		query.append(ORDER_BY_CLAUSE);
		log.info("Search Query Created After All the Checks :::"+ query);
		return addPaginationWrapper(query.toString(), preparedStatement, criteria);
	}
	
	private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
		if (values.isEmpty())
			queryString.append(" WHERE ");
		else {
			queryString.append(" AND");
		}
	}

	private String createQuery(Set<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}
	
	private String createQuery(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        int length = ids.size();
        for( int i = 0; i< length; i++){
            builder.append(" LOWER(?)");
            if(i != length -1) builder.append(",");
        }
        return builder.toString();
    }

	private void addToPreparedStatement(List<Object> preparedStatement, Set<String> ids) {
		preparedStatement.addAll(ids);
	}
	
	
	private void addToPreparedStatement(List<Object> preparedStmtList,List<String> ids)
    {
        ids.forEach(id ->{ preparedStmtList.add(id);});
    }

	/**
	 * 
	 * @param query
	 *            Query String
	 * @param preparedStmtList
	 *            Array of object for preparedStatement list
	 * @param criteria SearchCriteria
	 * @return It's returns query
	 */
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, SearchCriteria criteria) {
		Integer limit = config.getDefaultLimit();
		Integer offset = config.getDefaultOffset();
		if (criteria.getLimit() == null && criteria.getOffset() == null)
			limit = config.getMaxLimit();
//
//		if (criteria.getLimit() != null && criteria.getLimit() <= config.getDefaultLimit())
//			limit = criteria.getLimit();
//
//		if (criteria.getLimit() != null && criteria.getLimit() > config.getDefaultOffset())
//			limit = config.getDefaultLimit();
		
		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxLimit())
			limit = config.getMaxLimit();
		
		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		preparedStmtList.add(offset);
		preparedStmtList.add(limit + offset);
		return PAGINATION_WRAPPER.replace("{}",query);
	}
	
	private void addORClauseIfRequired(List<Object> values, StringBuilder queryString){
		if (values.isEmpty())
			queryString.append(" WHERE ");
		else {
			queryString.append(" OR");
		}
	}
	
	public String getWCPlainSearchQuery(SearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(WATER_SEARCH_QUERY);

        Set<String> ids = criteria.getIds();
        if (!CollectionUtils.isEmpty(ids)) {
            addClauseIfRequired(preparedStmtList,builder);
            builder.append(" conn.id IN (").append(createQuery(ids)).append(")");
            addToPreparedStatement(preparedStmtList, ids);
        }

        return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

    }
}
