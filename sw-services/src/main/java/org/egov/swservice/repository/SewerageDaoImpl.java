package org.egov.swservice.repository;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.swservice.config.SWConfiguration;
import org.egov.swservice.repository.rowmapper.OpenSewerageRowMapper;
import org.egov.swservice.web.models.SearchCriteria;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.producer.SewarageConnectionProducer;
import org.egov.swservice.repository.builder.SWQueryBuilder;
import org.egov.swservice.repository.rowmapper.SewerageRowMapper;
import org.egov.swservice.util.SWConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class SewerageDaoImpl implements SewerageDao {

	@Autowired
	private SewarageConnectionProducer sewarageConnectionProducer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SWQueryBuilder swQueryBuilder;

	@Autowired
	private SewerageRowMapper sewarageRowMapper;

	@Autowired
	private OpenSewerageRowMapper openSewerageRowMapper;

	@Autowired
	private SWConfiguration swConfiguration;

	@Value("${egov.sewarageservice.createconnection.topic}")
	private String createSewarageConnection;

	@Value("${egov.sewarageservice.updateconnection.topic}")
	private String updateSewarageConnection;

	@Override
	public void saveSewerageConnection(SewerageConnectionRequest sewerageConnectionRequest) {
		sewarageConnectionProducer.push(createSewarageConnection, sewerageConnectionRequest);
	}

	@Override
	public List<SewerageConnection> getSewerageConnectionList(SearchCriteria criteria, RequestInfo requestInfo) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = swQueryBuilder.getSearchQueryString(criteria, preparedStatement, requestInfo);
		if (query == null)
			return Collections.emptyList();
		Boolean isOpenSearch = isSearchOpen(requestInfo.getUserInfo());
		List<SewerageConnection> sewerageConnectionList = new ArrayList<>();
		if (isOpenSearch)
			sewerageConnectionList = jdbcTemplate.query(query, preparedStatement.toArray(), openSewerageRowMapper);
		else
			sewerageConnectionList = jdbcTemplate.query(query, preparedStatement.toArray(), sewarageRowMapper);

		if (sewerageConnectionList == null) {
			return Collections.emptyList();
		}
		return sewerageConnectionList;
	}

	public Boolean isSearchOpen(User userInfo) {

		return userInfo.getType().equalsIgnoreCase("SYSTEM")
				&& userInfo.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()).contains("ANONYMOUS");
	}

	public void updateSewerageConnection(SewerageConnectionRequest sewerageConnectionRequest,
			boolean isStateUpdatable) {
		log.info("SW application state updatable flag:" + isStateUpdatable);
		log.info("SW application request before update:" + sewerageConnectionRequest);
		if (isStateUpdatable) {
			sewarageConnectionProducer.push(updateSewarageConnection, sewerageConnectionRequest);
		} else {
			sewarageConnectionProducer.push(swConfiguration.getWorkFlowUpdateTopic(), sewerageConnectionRequest);
		}
	}

	/**
	 * push object for edit notification
	 * 
	 * @param sewerageConnectionRequest - Sewerage Connection Request Object
	 */
	public void pushForEditNotification(SewerageConnectionRequest sewerageConnectionRequest) {
		if (!SWConstants.EDIT_NOTIFICATION_STATE
				.contains(sewerageConnectionRequest.getSewerageConnection().getProcessInstance().getAction())) {
			sewarageConnectionProducer.push(swConfiguration.getEditNotificationTopic(), sewerageConnectionRequest);
		}
	}

	/**
	 * Enrich file store Id's
	 * 
	 * @param sewerageConnectionRequest - Sewerage Connection Request Object
	 */
	public void enrichFileStoreIds(SewerageConnectionRequest sewerageConnectionRequest) {
		sewarageConnectionProducer.push(swConfiguration.getFileStoreIdsTopic(), sewerageConnectionRequest);
	}

	/**
	 * Save file store Id's
	 * 
	 * @param sewerageConnectionRequest - Sewerage Connection Request Object
	 */
	public void saveFileStoreIds(SewerageConnectionRequest sewerageConnectionRequest) {
		sewarageConnectionProducer.push(swConfiguration.getSaveFileStoreIdsTopic(), sewerageConnectionRequest);
	}

	public void updateSewerageApplicationStatus(String id, String status) {

		Object[] params = { status, id };

		int[] types = { Types.VARCHAR, Types.VARCHAR };

		jdbcTemplate.update(SWQueryBuilder.UPDATE_DISCONNECT_STATUS, params, types);

	}
	
	@Override
	public List<String> fetchSewerageConnectionIds(SearchCriteria criteria){

        List<Object> preparedStmtList = new ArrayList<>();
        preparedStmtList.add(criteria.getTenantId());
        preparedStmtList.add(criteria.getOffset());
        preparedStmtList.add(criteria.getLimit());

        return jdbcTemplate.query("SELECT id from eg_sw_connection where tenantid=? ORDER BY createdtime offset " +
                        " ? " +
                        "limit ? ",
                preparedStmtList.toArray(),
                new SingleColumnRowMapper<>(String.class));
    }
	
	@Override
	public List<SewerageConnection> getPlainSewerageConnectionSearch(SearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = swQueryBuilder.getSCPlainSearchQuery(criteria, preparedStmtList);
        log.info("Query: " + query);
        List<SewerageConnection> sewerageconnection =  jdbcTemplate.query(query, preparedStmtList.toArray(), sewarageRowMapper);
        return sewerageconnection;
    }

}
