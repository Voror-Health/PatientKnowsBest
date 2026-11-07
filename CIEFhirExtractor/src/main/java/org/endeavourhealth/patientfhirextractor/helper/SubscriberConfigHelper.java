package org.endeavourhealth.patientfhirextractor.helper;


import org.endeavourhealth.core.database.dal.usermanager.caching.ProjectCache;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.ProjectEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class SubscriberConfigHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriberConfigHelper.class);


    public static Map<String, List<String>> getConfigNamesAndPublisherCodes(String[] projectIds) throws Exception {
        Map<String, List<String>> configNamesAndPublisherCodes = new HashMap<String, List<String>>();
        for (String projectId: projectIds) {

            List<String> publisherOdsCodes = ProjectCache.getAllPublishersForValidProject(projectId, true);
            LOG.trace("For project ID [" + projectId + "] got " + publisherOdsCodes.size() + " publisher ODS codes: " + publisherOdsCodes);
            ProjectEntity project = ProjectCache.getProjectDetails(projectId);
            String subscriberConfigName=project.getConfigName();
            configNamesAndPublisherCodes.put(subscriberConfigName,publisherOdsCodes);
        }

        return configNamesAndPublisherCodes;
    }
}