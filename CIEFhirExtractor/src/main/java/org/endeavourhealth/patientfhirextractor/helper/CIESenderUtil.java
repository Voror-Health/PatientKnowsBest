package org.endeavourhealth.patientfhirextractor.helper;

import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class CIESenderUtil {


    private static final Logger LOG = LoggerFactory.getLogger(CIESenderUtil.class);

    public static String sendResource(String json,String resourceType,String method, CIEJdbcDAL cieExtractsDal, String regTokenPublicOrg, String regAuthAccess){

      try {
            URL url = null;
            try {
                JSONParser parser = new JSONParser();
                JSONObject jsonObj = (JSONObject) parser.parse(json);
                String resourceId = jsonObj.get("id").toString();
            if (method.equals("POST")) {
                url = new URL(getPostURL(resourceType));
            } else if (method.equals("PUT")) {
                url = new URL(getPostURL(resourceType) + "/" + resourceId);
            }
        } catch (Exception e){
                LOG.info("Errored");
        }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            String token=null;

            //String regTokenURL=  CIEFhirExtractorEndpoint.runConnections.get("regTokenURL").asText();
            String fhirTokenURL= CIEFhirExtractorEndpoint.runConnections.get("fhirTokenURL").asText();
           // String regTokenPublicOrg= CIEFhirExtractorEndpoint.runConnections.get("regTokenPublicOrg").asText();
            String fhirTokenSec= CIEFhirExtractorEndpoint.runConnections.get("fhirTokenSec").asText();
            String clientId= CIEFhirExtractorEndpoint.runConnections.get("clientId").asText();

          token = getToken(fhirTokenURL, "client_credentials", clientId, fhirTokenSec);
          /*
            if(resourceType.equals(ResourceConstants.NewPatient)){
                token = getTokenForNewRegistration(regTokenURL, "client_credentials", regAuthAccess);
                conn.setRequestProperty("X-Org-Public-Id",regTokenPublicOrg);
            }
            else {
                token = getToken(fhirTokenURL, "client_credentials", clientId, fhirTokenSec);
            }
           */
            conn.setRequestProperty("Authorization",token);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            String msg = getMsg(conn);
            if (msg != null) {
                return msg;
            }

            // This method is record  to the FHIR Data before we send to PKB
                try {
                    insertOrUpdateFhir(resourceType, json, cieExtractsDal,method);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

             BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            try {
                String output;

                while ((output = br.readLine()) != null) {
                    // LOG.debug(output);
                }
                br.close();
                conn.disconnect();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success";
    }

    private static String getMsg(HttpURLConnection conn) throws IOException {
        if (conn.getResponseCode() >250 || conn.getResponseCode()<200 ) {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getErrorStream())));
            String msg="";
            String output;

            while ((output = br.readLine()) != null) {
                msg=msg+output;
            }
            return ("Failed : HTTP error code : "
                    + conn.getResponseCode()+":"+conn.getResponseMessage()+msg);
        }
        return null;
    }

    private  static String getPostURL(String resourceType) {

        String url;

        switch(resourceType)
        {
            case ResourceConstants.Organization: url= CIEFhirExtractorEndpoint.runConnections.get("postOrganization").asText(); break;
            case ResourceConstants.Patient : url= CIEFhirExtractorEndpoint.runConnections.get("postPatientURL").asText(); break;
            case ResourceConstants.Condition: url= CIEFhirExtractorEndpoint.runConnections.get("postConditionURL").asText(); break;
            case ResourceConstants.MedicationStatement: url= CIEFhirExtractorEndpoint.runConnections.get("postMedicationStatementURL").asText(); break;
            case ResourceConstants.Medication: url= CIEFhirExtractorEndpoint.runConnections.get("postMedicationURL").asText();break;
            case ResourceConstants.Practitioner: url= CIEFhirExtractorEndpoint.runConnections.get("postPractitionerURL").asText(); break;
           // case ResourceConstants.PractitionerRole: url= CIEFhirExtractorEndpoint.runConnections.get("postPractitionerRoleURL").asText(); break;
            case ResourceConstants.Appointment: url= CIEFhirExtractorEndpoint.runConnections.get("postAppointmentURL").asText(); break;
          //  case ResourceConstants.NewPatient: url= CIEFhirExtractorEndpoint.runConnections.get("postNewPatientURL").asText(); break;
            default: url=null;
        }
        return url;
    }

    private static String encodeParam(String data) throws UnsupportedEncodingException {
        String result = "";
        try {
            result = URLEncoder.encode(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static byte[] getParamsByte(Map<String, Object> params) throws UnsupportedEncodingException {
        byte[] result = null;
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(encodeParam(param.getKey()));
            postData.append('=');
            postData.append(encodeParam(String.valueOf(param.getValue())));
        }
        try {
            result = postData.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getToken(String tokenUrl,String gType,String clientId,String clientSec) throws IOException {
        String token = "Bearer ";
        try {
            URL url = new URL(tokenUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setUseCaches(false);

            Map<String, Object> queryParams =new HashMap<>();
            queryParams.put("grant_type",gType);
            queryParams.put("client_id",clientId);
            queryParams.put("client_secret",clientSec);
            byte[] postDataBytes = getParamsByte(queryParams);
            httpConn.getOutputStream().write(postDataBytes);

            // Check the http status
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = httpConn.getInputStream().read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                String tokenResponse = result.toString("UTF-8");
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(tokenResponse);
                //convert to json and read token todo
                token = token + (String) json.get("access_token");
                httpConn.disconnect();
            } else {

                throw new IOException("Server returned non-OK status: " + status);
            }
            httpConn.disconnect();
        }

        catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return token;
    }

    /*
    public static String getTokenForNewRegistration(String tokenUrl,String gType, String regAuthAccess) throws IOException {
        String token = "Bearer ";
       // String regAuthAccess= CIEFhirExtractorEndpoint.runConnections.get("regAuthAccess").asText();

        try {
            URL url = new URL(tokenUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Authorization", "Basic " + regAuthAccess);
            httpConn.setUseCaches(false);

            Map<String, Object> queryParams =new HashMap<>();
            queryParams.put("grant_type",gType);
            byte[] postDataBytes = getParamsByte(queryParams);
            httpConn.getOutputStream().write(postDataBytes);


            // Check the http status
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = httpConn.getInputStream().read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                String tokenResponse = result.toString("UTF-8");
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(tokenResponse);
                //convert to json and read token todo
                token = token + (String) json.get("access_token");
                httpConn.disconnect();
            } else {

                throw new IOException("Server returned non-OK status: " + status);
            }
            httpConn.disconnect();
        }

        catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return token;
    } */

    public static String deleteResource(String resourceId, String resourceType) {
        try {
            URL url = null;
            try {
                url = new URL(getPostURL(resourceType) + "/" + resourceId);
            } catch (Exception e){
                LOG.info("Errored");
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            String token=null;

            String fhirTokenURL= CIEFhirExtractorEndpoint.runConnections.get("fhirTokenURL").asText();
            String fhirTokenSec= CIEFhirExtractorEndpoint.runConnections.get("fhirTokenSec").asText();
            String clientId= CIEFhirExtractorEndpoint.runConnections.get("clientId").asText();

            token = getToken(fhirTokenURL, "client_credentials", clientId, fhirTokenSec);

            conn.setRequestProperty("Authorization",token);

            String msg = getMsg(conn);
            if (msg != null) return msg;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success";
    }

    // This method is to record the FHIR Data before we send to PKB
    public static void insertOrUpdateFhir(String resourceType, String request, CIEJdbcDAL cieExtractsDal, String method) throws Exception {

        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(request);
            String resourceId = jsonObj.get("id").toString();
            if (method.equals("POST")) {
                if (cieExtractsDal.checkIfReferenceExists(resourceId, resourceType)) {
                    String info = resourceId + "  for  " + resourceType+ "  Resource already exists. Cannot be inserted.";
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, info);
                    LOG.info(info);
                } else {
                    cieExtractsDal.mockSendToDB(resourceId, resourceType, request);
                    LOG.info(resourceId + "  for  " + resourceType + "  Resource saved successfully");
                }
            } else {
                if(cieExtractsDal.checkIfReferenceExists(resourceId,resourceType)){
                    cieExtractsDal.updateReference(resourceId, resourceType, request);
                    LOG.info(resourceId + "  for  " + resourceType+ " Resource Updated successfully");
                } else {
                    String info = resourceId + "  for  " + resourceType+ "  Resource does not exist. Cannot be updated.";
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, info);
                    LOG.info(info);
                }
            }

        } catch (Exception e) {
            LOG.info("Errored");
        }

    }
}

