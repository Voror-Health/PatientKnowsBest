package org.endeavourhealth.patientfhirextractor.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiParam;
import org.endeavourhealth.patientfhirextractor.process.CIEDeltaProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

@Path("/cie_fhir_extractor")
public class CIEFhirExtractorEndpoint {
    //Initialize logger
    private static final Logger LOG = LoggerFactory.getLogger(CIEFhirExtractorEndpoint.class);
    public static final int BULK = 1;
    public static final int DELTA = 2;
    public static final int DELTA_RC = 3;
    public static final int REPROCESS = 4;
    public static final int REBULK = 5;

    public static Boolean bulkRunFlag = false;
    public static Boolean deltaRunFlag = false;
    public static Boolean deltaRCRunFlag = false;
    public static Boolean rebulkRunFlag = false;

    public static String runmode="live";

    public static JsonNode runConnections ;


    public static boolean isInterrupted(String type) {
        if(type.equalsIgnoreCase("BULK"))
            return !bulkRunFlag;
        else if(type.equalsIgnoreCase("DELTA"))
            return !deltaRunFlag;
        else if(type.equalsIgnoreCase("DELTARC"))
            return !deltaRCRunFlag;
        else if(type.equalsIgnoreCase("REBULK"))
            return !rebulkRunFlag;
        return true;
    }

    public static void setRunflag(String type,boolean flag) {
        if(type.equalsIgnoreCase("BULK"))
            bulkRunFlag=flag;
        else if(type.equalsIgnoreCase("DELTA"))
            deltaRunFlag=flag;
        else if(type.equalsIgnoreCase("DELTARC"))
            deltaRCRunFlag = flag;
        else if(type.equalsIgnoreCase("REBULK"))
            rebulkRunFlag=flag;

    }

    @GET()
    @Path("/bulk/start")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response startBulk(@Context HttpServletRequest httpServletRequest,
                              @Context UriInfo uriInfo,
                              @Context SecurityContext sc
            , @ApiParam(value = "Project ID") @HeaderParam(value = "ProjectID") String headerProjectIds
    ) throws Exception {

        return (new CieExtractor()).processRequest(httpServletRequest, uriInfo, headerProjectIds, BULK);
    }

    @GET
    @Path("bulk/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response stopBulk() {

        setRunflag("Bulk",false);
        return Response.status(Response.Status.OK).build();
    }

    @GET()
    @Path("delta/start")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response startDelta(@Context HttpServletRequest httpServletRequest,
                               @Context UriInfo uriInfo,
                               @Context SecurityContext sc
            , @ApiParam(value = "Project ID") @HeaderParam(value = "ProjectID") String headerProjectIds
    ) throws Exception {
        return (new CieExtractor()).processRequest(httpServletRequest, uriInfo, headerProjectIds, DELTA);

    }

    @GET
    @Path("delta/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response stopDelta() {
        setRunflag("Delta",false);
        return Response.status(Response.Status.OK).build();
    }

    @GET()
    @Path("re_consented_delta/start")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response startDeltaRC(@Context HttpServletRequest httpServletRequest,
                                 @Context UriInfo uriInfo,
                                 @Context SecurityContext sc
            , @ApiParam(value = "Project ID") @HeaderParam(value = "ProjectID") String headerProjectIds
    ) throws Exception {
        return (new CieExtractor()).processRequest(httpServletRequest, uriInfo, headerProjectIds, DELTA_RC);

    }

    @GET
    @Path("re_consented_delta/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response stopDeltaRC() {

        setRunflag("DELTARC",false);
        return Response.status(Response.Status.OK).build();
    }

    @GET()
    @Path("/rebulk/start")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response startRebulk(@Context HttpServletRequest httpServletRequest,
                                @Context UriInfo uriInfo,
                                @Context SecurityContext sc
            , @ApiParam(value = "Project ID") @HeaderParam(value = "ProjectID") String headerProjectIds
    ) throws Exception {
        return (new CieExtractor()).processRequest(httpServletRequest, uriInfo, headerProjectIds, REBULK);
    }

    @GET
    @Path("rebulk/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response stopRebulk() {
        setRunflag("REBULK",false);
        return Response.status(Response.Status.OK).build();
    }

    @GET()
    @Path("reprocess/start")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response startReprocess(@Context HttpServletRequest httpServletRequest,
                                   @Context UriInfo uriInfo,
                                   @Context SecurityContext sc) throws Exception {
        return (new CieExtractor()).processRequest(httpServletRequest, uriInfo,null, REPROCESS);

    }


}
