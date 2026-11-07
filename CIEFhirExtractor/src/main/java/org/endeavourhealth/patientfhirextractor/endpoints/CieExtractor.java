package org.endeavourhealth.patientfhirextractor.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.core.database.dal.usermanager.caching.UserCache;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.models.CIEExtractorConfig;
import org.endeavourhealth.patientfhirextractor.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.regex.Pattern;


public class CieExtractor {
    public static final String APP_NAME = "cie-fhir-extractor";
    //Initialize logger
    private static final Logger LOG = LoggerFactory.getLogger(CieExtractor.class);


    public String validateUserWithProjectID(String userUuid, String projectId) {

        try {
            //validating user application policy , dsm project-application policy and policy attributes
            boolean canUseApi = UserCache.getUserProjectApplicationAccess(userUuid, projectId, APP_NAME);
            if (!canUseApi) {
                return "User not permitted to use DDS Subscriber API for project " + projectId;
            }

            return null;

        } catch (Exception ex) {
            //log any exception
            LOG.error("Error validating user " + userUuid + " for project " + projectId, ex);

            //any exception should be returned as a processing error
            String err = ex.getMessage();
            return "Unexpected error validating project " + projectId + ": " + ex.getMessage();
        }
    }

    public Response processRequest(HttpServletRequest httpServletRequest, UriInfo uriInfo, String headerProjectIds, int requestType) {
        //read user id from header
        //e269f5f7-1f82-47b6-8f9e-346dea6931dd
        String userUuid = httpServletRequest.getHeader("user_id");
        Response response = null;

        if (requestType == CIEFhirExtractorEndpoint.REPROCESS) {
            try {
                java.sql.Date startDateTime = null;
                java.sql.Date endDateTime = null;

                //read subject ,code from Query parameters and validate it for null check
                MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
                if (params.keySet().size() == 0) {
                    CIEReProcess cieReProcess = new CIEReProcess(readCIEConfigurations());
                    response = cieReProcess.process();
                } else {
                    for (String key : params.keySet()) {
                        String value = params.getFirst(key);
                        LOG.info("Request parameter [" + key + "] = [" + value + "]");

                        if (key.equalsIgnoreCase("startdatetime")) {
                            if (Strings.isNullOrEmpty(value)) {
                                return Response.status(400).entity("Missing code parameter '" + key + "'").build();
                            }
                            try {
                                startDateTime = java.sql.Date.valueOf(value);
                            } catch (Exception e) {
                                return Response.status(400).entity("invalid start date format it should be :(yyyy-[m]m-[d]d)").build();
                            }
                        } else if (key.equalsIgnoreCase("enddatetime")) {
                            if (Strings.isNullOrEmpty(value)) {
                                return Response.status(400).entity("Missing code parameter '" + key + "'").build();
                            }
                            try {
                                endDateTime = java.sql.Date.valueOf(value);
                            } catch (Exception e) {
                                return Response.status(400).entity("invalid end date format it should be :(yyyy-[m]m-[d]d)").build();
                            }

                        } else {
                            return Response.status(400).entity("Invalid parameter '" + key + "'").build();
                        }
                    }
                        CIEReProcess cieReProcess = new CIEReProcess(readCIEConfigurations());
                        response = cieReProcess.process(startDateTime, endDateTime);

                }
            } catch (Exception e) {
                return Response.status(503).entity("Error while processing :" + e.getMessage()).build();
            }
        }
        else {

            String[] projectIds = headerProjectIds.split(Pattern.quote("|"));
            if (projectIds.length == 0) {
                return Response.status(400).entity("Missing ProjectID from request headers").build();
            }
            try {
                for (String projectId : projectIds) {
                    String err = validateUserWithProjectID(userUuid, projectId);
                    if (null != err) {
                        return Response.status(400).entity("Invalid ProjectID " + projectId).build();

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Response.status(400).entity("Invalid ProjectID from request headers").build();
                //b216b368-f093-4b09-9703-3a2c4d6b122e
            }


            if (requestType == CIEFhirExtractorEndpoint.BULK) {
                try {

                    try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {

                        cieExtractsDal.setCieProcessConnection();
                        response= checkAndPutProcessInProgress("Bulk",cieExtractsDal);
                        if(null!=response) return response;

                    }
                    CIEBulkProcess cieBulkProcess = new CIEBulkProcess(readCIEConfigurations());
                    response = cieBulkProcess.process(projectIds);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    return Response.status(503).entity("Unknown Error Occured").build();
                }

            } else if (requestType == CIEFhirExtractorEndpoint.DELTA) {
                try {
                    try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {

                        cieExtractsDal.setCieProcessConnection();

                        response= checkAndPutProcessInProgress("Delta",cieExtractsDal);
                        if(null!=response) return response;

                    }
                    CIEDeltaProcess cieDeltaProcess = new CIEDeltaProcess(readCIEConfigurations());
                    response = cieDeltaProcess.process(projectIds);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    return Response.status(503).entity("Unknown Error Occured").build();
                }

            } else if (requestType == CIEFhirExtractorEndpoint.DELTA_RC) {
                try {
                    try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {

                        cieExtractsDal.setCieProcessConnection();
                        response= checkAndPutProcessInProgress("DeltaRC",cieExtractsDal);
                        if(null!=response) return response;

                    }
                    CIEDeltaRCProcess cieDeltaRCProcess = new CIEDeltaRCProcess(readCIEConfigurations());
                    response = cieDeltaRCProcess.process(projectIds);

                } catch (Exception ex) {
                    return Response.status(503).entity("Unknown Error Occured").build();
                }

            } else if (requestType == CIEFhirExtractorEndpoint.REBULK) {
                try {
                    MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
                    if (params.keySet().size() > 0) {

                        for (String key : params.keySet()) {
                            String value = params.getFirst(key);
                            LOG.info("Request parameter [" + key + "] = [" + value + "]");
                            if (key.equalsIgnoreCase("resourceType")) {
                                if (Strings.isNullOrEmpty(value)) {
                                    return Response.status(400).entity("Missing code parameter '" + key + "'").build();
                                }
                                try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {

                                    cieExtractsDal.setCieProcessConnection();
                                    response= checkAndPutProcessInProgress("Rebulk",cieExtractsDal);
                                    if(null!=response) return response;

                                }
                                CIERebulkProcess cieRebulkProcess = new CIERebulkProcess(readCIEConfigurations());
                                response = cieRebulkProcess.process(projectIds, value);
                            } else {
                                return Response.status(400).entity("Invalid query parameter '" + key + "'").build();
                            }
                        }
                    } else {
                        return Response.status(400).entity("Missing required query parameter 'resourceType'").build();

                    }

                } catch (Exception ex) {

                    return Response.status(503).entity("Unknown Error Occurred").build();
                }

            }
        }
        return response;
    }

    private Response checkAndPutProcessInProgress(String processName, CIEJdbcDAL cieExtractsDal) throws Exception {
        boolean inProgress = cieExtractsDal.isInProgress(processName);
        if (inProgress) {
            return Response.status(409).entity(processName+" already in Progress").build();
        }
        cieExtractsDal.setProcessStatus(processName, "InProgress");
        CIEFhirExtractorEndpoint.setRunflag(processName,true);
        return null;
    }

    private CIEExtractorConfig readCIEConfigurations() throws IOException {
        JsonNode cieConfigurations = ConfigManager.getConfigurationAsJson("cie_run_configuration");
        CIEExtractorConfig cieExtractorConfig = new CIEExtractorConfig();
        assert cieConfigurations != null;
        cieExtractorConfig.setBulkPriorities(cieConfigurations.get("bulkPriority"));
        cieExtractorConfig.setDeltaPriorities(cieConfigurations.get("deltaPriority"));
        cieExtractorConfig.setDeltaDeletePriorities(cieConfigurations.get("deltaDeletePriority"));
        cieExtractorConfig.setMaxQueueBeforeBlocking(cieConfigurations.get("maxQueuedBeforeBlocking").asInt());
        cieExtractorConfig.setMaxThreadPoolSize(cieConfigurations.get("maxThreadPoolSize").asInt());
        cieExtractorConfig.setMaxOrgsPerThread(cieConfigurations.get("maxOrgsPerThread").asInt());
        cieExtractorConfig.setEmailIds(cieConfigurations.get("email_ids"));
        CIEFhirExtractorEndpoint.runmode=cieConfigurations.get("runmode").asText();
        CIEFhirExtractorEndpoint.runConnections=cieConfigurations.get(CIEFhirExtractorEndpoint.runmode);
        return cieExtractorConfig;
    }
}
