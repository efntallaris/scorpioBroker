package eu.neclab.ngsildbroker.historymanager.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;



import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.constants.NGSIConstants;
import eu.neclab.ngsildbroker.commons.datatypes.QueryParams;
import eu.neclab.ngsildbroker.commons.datatypes.QueryResult;
import eu.neclab.ngsildbroker.commons.datatypes.RestResponse;
import eu.neclab.ngsildbroker.commons.enums.ErrorType;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import eu.neclab.ngsildbroker.commons.ldcontext.ContextResolverBasic;
import eu.neclab.ngsildbroker.commons.ngsiqueries.ParamsResolver;
import eu.neclab.ngsildbroker.commons.tools.HttpUtils;
import eu.neclab.ngsildbroker.historymanager.repository.HistoryDAO;
import eu.neclab.ngsildbroker.historymanager.service.HistoryService;
import eu.neclab.ngsildbroker.historymanager.utils.Validator;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import eu.neclab.ngsildbroker.historymanager.repository.TCSourceDAO;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/ngsi-ld/v1/temporal/entities")
public class HistoryController {

	private final static Logger logger = LoggerFactory.getLogger(HistoryController.class);

	@Autowired
	ParamsResolver paramsResolver;

	@Autowired
	HistoryDAO historyDAO;

	@Autowired
	HistoryService historyService;
	@Autowired
	ContextResolverBasic contextResolver;
	@Value("${atcontext.url}")
	String atContextServerUrl;

	@Autowired
	@Qualifier("qmtcsourcedao")
	TCSourceDAO cSourceDAO;

	@Autowired
	@Qualifier("hsrestTemp")
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	private HttpUtils httpUtils;

	@PostConstruct
	private void setup() {
		this.httpUtils = HttpUtils.getInstance(contextResolver);
	}

	@PostMapping
	public ResponseEntity<byte[]> createTemporalEntity(HttpServletRequest request,
			@RequestBody(required = false) String payload) {
		try {
			logger.trace("createTemporalEntity :: started");
			Validator.validateTemporalEntity(payload);

			String resolved = httpUtils.expandPayload(request, payload, AppConstants.HISTORY_URL_ID);

			URI uri = historyService.createTemporalEntityFromBinding(resolved);
			logger.trace("createTemporalEntity :: completed");
			return ResponseEntity.status(HttpStatus.CREATED).header("Location", uri.toString()).body(uri.toString().getBytes());
		} catch (ResponseException exception) {
			logger.error("Exception", exception);
			return ResponseEntity.status(exception.getHttpStatus()).body(new RestResponse(exception).toJsonBytes());
		} catch (Exception exception) {
			logger.error("Exception", exception);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, exception.getLocalizedMessage()).toJsonBytes());
		}
	}


	/**
	 * making http call to all discovered csources async.
	 * 
	 * @param endpointsList
	 * @param query
	 * @return List<Entity>
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws URISyntaxException
	 * @throws ResponseException
	 * @throws IOException
	 */
	private List<String> getDataFromCsources(Set<Callable<String>> callablesCollection)
			throws ResponseException, Exception {
		List<String> allDiscoveredEntities = new ArrayList<String>();
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		List<Future<String>> futures = executorService.invokeAll(callablesCollection);
		// TODO: why sleep?
		// Thread.sleep(5000);
		for (Future<String> future : futures) {
			logger.trace("future.isDone = " + future.isDone());
			List<String> entitiesList = new ArrayList<String>();
			try {
				String response = (String) future.get();
				logger.debug("response from invoke all ::" + response);
				if (!("[]").equals(response) && response != null) {
					JsonNode jsonNode = objectMapper.readTree(response);
					for (int i = 0; i <= jsonNode.size(); i++) {
						if (jsonNode.get(i) != null && !jsonNode.isNull()) {
							String payload = contextResolver.expand(jsonNode.get(i).toString(), null, true, AppConstants.ENTITIES_URL_ID);// , linkHeaders);
							entitiesList.add(payload);
						}
					}
				}
			} catch (JsonSyntaxException | ExecutionException e) {
				logger.error("Exception  ::", e);
			}
			allDiscoveredEntities.addAll(entitiesList);
		}
		executorService.shutdown();
		logger.trace("getDataFromCsources() completed ::");
		return allDiscoveredEntities;
	}

	public ResponseEntity<byte[]> generateReply(HttpServletRequest request, QueryResult qResult, boolean forceArray)
			throws ResponseException {
		String nextLink = generateNextLink(request, qResult);
		String prevLink = generatePrevLink(request, qResult);
		ArrayList<String> additionalLinks = new ArrayList<String>();
		if (nextLink != null) {
			additionalLinks.add(nextLink);
		}
		if (prevLink != null) {
			additionalLinks.add(prevLink);
		}

		HashMap<String, List<String>> additionalHeaders = new HashMap<String, List<String>>();
		if (!additionalLinks.isEmpty()) {
			additionalHeaders.put(HttpHeaders.LINK, additionalLinks);
		}

		return httpUtils.generateReply(request, "[" + String.join(",", qResult.getDataString()) + "]",
				additionalHeaders, null, forceArray);
	}

	private String generateNextLink(HttpServletRequest request, QueryResult qResult) {
		if (qResult.getResultsLeftAfter() == null || qResult.getResultsLeftAfter() <= 0) {
			return null;
		}
		return generateFollowUpLinkHeader(request, qResult.getOffset() + qResult.getLimit(), qResult.getLimit(),
				qResult.getqToken(), "next");
	}

	private String generateFollowUpLinkHeader(HttpServletRequest request, int offset, int limit, String token,
			String rel) {

		StringBuilder builder = new StringBuilder("</");
		builder.append("?");

		for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			String[] values = entry.getValue();
			String key = entry.getKey();
			if (key.equals("offset")) {
				continue;
			}
			if (key.equals("qtoken")) {
				continue;
			}
			if (key.equals("limit")) {
				continue;
			}

			for (String value : values) {
				builder.append(key + "=" + value + "&");
			}

		}
		builder.append("offset=" + offset + "&");
		builder.append("limit=" + limit + "&");
		builder.append("qtoken=" + token + ">;rel=\"" + rel + "\"");
		return builder.toString();
	}

	private String generatePrevLink(HttpServletRequest request, QueryResult qResult) {
		if (qResult.getResultsLeftBefore() == null || qResult.getResultsLeftBefore() <= 0) {
			return null;
		}
		int offset = qResult.getOffset() - qResult.getLimit();
		if (offset < 0) {
			offset = 0;
		}
		int limit = qResult.getLimit();

		return generateFollowUpLinkHeader(request, offset, limit, qResult.getqToken(), "prev");
	}

	@GetMapping
	public ResponseEntity<byte[]> retrieveTemporalEntity(HttpServletRequest request) {
		String params = request.getQueryString();
		List<Object> linkHeaders = HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT);
		List<String> aggregatedResult = new ArrayList<String>();
		List<String> realResult;
		QueryResult result = new QueryResult(null, null, ErrorType.None, -2, true);


		//QueryResult result = new QueryResult(null, null, ErrorType.None, -1, true);
		try {
			logger.info(" Stratos:: retrieving temporal entity");
			logger.trace("retrieveTemporalEntity :: started");
			logger.info("params:" + params);
			if (params != null && !Validator.validate(params))
				throw new ResponseException(ErrorType.BadRequestData);

			QueryParams qp = paramsResolver.getQueryParamsFromUriQuery(request.getParameterMap(),
					HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT), true);
			if (qp == null) // invalid query
				throw new ResponseException(ErrorType.InvalidRequest);
			if (qp.getTimerel() == null || qp.getTime() == null) {
				throw new ResponseException(ErrorType.BadRequestData, "Time filter is required");
			}
			if (qp.getType() == null && qp.getAttrs() == null) {
				throw new ResponseException(ErrorType.BadRequestData, "Type or attrs is required");
			}

			logger.info("STRATOS IM HERE 2");
			ExecutorService executorService = Executors.newFixedThreadPool(2);

			Future<List<String>> futureStorageManager = executorService.submit(new Callable<List<String>>() {
				public List<String> call() throws Exception {
			logger.trace("Asynchronous Callable storage manager");
					//TAKE CARE OF PAGINATION HERE
					if (historyDAO != null) {

						return historyDAO.query(qp);
					} else {
						//return getFromStorageManager(DataSerializer.toJson(qp));
						return null;
					}
					//if (queryDAO != null) {
						//return queryDAO.query(qp);
					//} else {
						//return getFromStorageManager(DataSerializer.toJson(qp));
					//}
				}
			});

			Future<List<String>> futureContextRegistry = executorService.submit(new Callable<List<String>>() {
				public List<String> call() throws Exception {
					try {
						List<String> fromCsources = new ArrayList<String>();
						logger.info("STRATOS IM HERE 2");
						logger.trace("Asynchronous 1 context registry");
						List<String> brokerList;
						if (cSourceDAO != null) {
							brokerList = cSourceDAO.queryExternalCsources(qp);
						} else {
							//brokerList = getFromContextRegistry(DataSerializer.toJson(qp));
							brokerList = cSourceDAO.queryExternalCsources(qp);
						}
						for (String brokerInfo : brokerList) {
							logger.info("STRATOS STRING IS:"+brokerInfo);
						}
						Pattern p = Pattern.compile(NGSIConstants.NGSI_LD_ENDPOINT_REGEX);
						Matcher m;
						Set<Callable<String>> callablesCollection = new HashSet<Callable<String>>();
						for (String brokerInfo : brokerList) {
							m = p.matcher(brokerInfo);
							m.find();
							String uri = m.group(1);
							logger.debug("url " + uri.toString() + "/ngsi-ld/v1/temporal/entities/?" + params);
							logger.info("STRATOS url " + uri.toString() + "/ngsi-ld/v1/temporal/entities/?" + params);
							Callable<String> callable = () -> {
								HttpHeaders headers = new HttpHeaders();
								for (Object link : linkHeaders) {
									headers.add("Link", "<" + link.toString()
											+ ">; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"");
								}

								HttpEntity entity = new HttpEntity<>(headers);

								String result = restTemplate.exchange(uri + "/ngsi-ld/v1/temporal/entities/?" + params,
										HttpMethod.GET, entity, String.class).getBody();

								logger.debug("http call result :: ::" + result);
								return result;
							};
							callablesCollection.add(callable);

						}
						fromCsources = getDataFromCsources(callablesCollection);
						logger.debug("csource call response :: ");
						// fromCsources.forEach(e -> logger.debug(e));

						return fromCsources;
					} catch (Exception e) {
						e.printStackTrace();
						logger.error(
								"No reply from registry. Looks like you are running without a context source registry.");
						logger.error(e.getMessage());
						return null;
					}
				}
			});

//			// Csources response

			executorService.shutdown();


//
//			// storage response
			logger.trace("storage task status completed :: " + futureStorageManager.isDone());
			List<String> fromStorage = (List<String>) futureStorageManager.get();
			List<String> fromCsources = (List<String>) futureContextRegistry.get();
			aggregatedResult.addAll(fromStorage);
			if (fromCsources != null) {
				aggregatedResult.addAll(fromCsources);
			}
			realResult = aggregatedResult;
			result.setDataString(realResult);
//			result.setqToken(qToken);
//			result.setLimit(limit);
//			result.setOffset(offset);
//			result.setResultsLeftAfter(dataLeft);
//			result.setResultsLeftBefore(offset);
			logger.trace("retrieveTemporalEntity :: completed");
			QueryResult qResult = result;
			return generateReply(request, qResult, true);
			//return httpUtils.generateReply(request, historyDAO.getListAsJsonArray(historyDAO.query(qp)));
			//return httpUtils.generateReply(request, result);
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

	@GetMapping("/{entityId}")
	public ResponseEntity<byte[]> retrieveTemporalEntityById(HttpServletRequest request,
			@PathVariable("entityId") String entityId) {
		String params = request.getQueryString();
		try {
			logger.info(" Stratos:: retrieving temporal entity with id " + entityId);
			logger.trace("retrieveTemporalEntityById :: started " + entityId);
			logger.debug("entityId : " + entityId);
			if (params != null && !Validator.validate(params))
				throw new ResponseException(ErrorType.BadRequestData);

			QueryParams qp = paramsResolver.getQueryParamsFromUriQuery(request.getParameterMap(),
					HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT), true);
			qp.setId(entityId);
			logger.trace("retrieveTemporalEntityById :: completed");
			return httpUtils.generateReply(request, historyDAO.getListAsJsonArray(historyDAO.query(qp)));
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

	@DeleteMapping("/{entityId}")
	public ResponseEntity<byte[]> deleteTemporalEntityById(HttpServletRequest request,
			@PathVariable("entityId") String entityId) {
		try {
			logger.trace("deleteTemporalEntityById :: started");
			logger.debug("entityId : " + entityId);
			historyService.delete(entityId, null, null,
					HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT));
			logger.trace("deleteTemporalEntityById :: completed");
			return ResponseEntity.noContent().build();
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

	@PostMapping("/{entityId}/attrs")
	public ResponseEntity<byte[]> addAttrib2TemopralEntity(HttpServletRequest request,
			@PathVariable("entityId") String entityId, @RequestBody(required = false) String payload) {
		try {
			logger.trace("addAttrib2TemopralEntity :: started");
			logger.debug("entityId : " + entityId);
			String resolved = httpUtils.expandPayload(request, payload, AppConstants.HISTORY_URL_ID);

			historyService.addAttrib2TemporalEntity(entityId, resolved);
			logger.trace("addAttrib2TemopralEntity :: completed");
			return ResponseEntity.noContent().build();
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

	@DeleteMapping("/{entityId}/attrs/{attrId}")
	public ResponseEntity<byte[]> deleteAttrib2TemporalEntity(HttpServletRequest request,
			@PathVariable("entityId") String entityId, @PathVariable("attrId") String attrId) {
		try {
			logger.trace("deleteAttrib2TemporalEntity :: started");
			logger.debug("entityId : " + entityId + " attrId : " + attrId);
			historyService.delete(entityId, attrId, null,
					HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT));
			logger.trace("deleteAttrib2TemporalEntity :: completed");
			return ResponseEntity.noContent().build();
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

	@PatchMapping("/{entityId}/attrs/{attrId}/{instanceId}")
	public ResponseEntity<byte[]> modifyAttribInstanceTemporalEntity(HttpServletRequest request,
			@PathVariable("entityId") String entityId, @PathVariable("attrId") String attrId,
			@PathVariable("instanceId") String instanceId, @RequestBody(required = false) String payload) {
		try {
			logger.trace("modifyAttribInstanceTemporalEntity :: started");
			logger.debug("entityId : " + entityId + " attrId : " + attrId + " instanceId : " + instanceId);

			String resolved = httpUtils.expandPayload(request, payload, AppConstants.HISTORY_URL_ID);

			// TODO : TBD- conflict between specs and implementation <mentioned no request
			// body in specs>
			historyService.modifyAttribInstanceTemporalEntity(entityId, resolved, attrId, instanceId,
					HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT));
			logger.trace("modifyAttribInstanceTemporalEntity :: completed");
			return ResponseEntity.noContent().build();
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

	@DeleteMapping("/{entityId}/attrs/{attrId}/{instanceId}")
	public ResponseEntity<byte[]> deleteAtrribInstanceTemporalEntity(HttpServletRequest request,
			@PathVariable("entityId") String entityId, @PathVariable("attrId") String attrId,
			@PathVariable("instanceId") String instanceId) {
		try {
			logger.trace("deleteAtrribInstanceTemporalEntity :: started");
			logger.debug("entityId : " + entityId + " attrId : " + attrId + " instanceId : " + instanceId);
			historyService.delete(entityId, attrId, instanceId,
					HttpUtils.parseLinkHeader(request, NGSIConstants.HEADER_REL_LDCONTEXT));
			logger.trace("deleteAtrribInstanceTemporalEntity :: completed");
			return ResponseEntity.noContent().build();
		} catch (ResponseException ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(ex.getHttpStatus()).body(new RestResponse(ex).toJsonBytes());
		} catch (Exception ex) {
			logger.error("Exception", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new RestResponse(ErrorType.InternalError, ex.getLocalizedMessage()).toJsonBytes());
		}
	}

}
