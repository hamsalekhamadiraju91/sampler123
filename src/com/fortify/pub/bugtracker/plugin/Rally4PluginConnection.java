package com.fortify.pub.bugtracker.plugin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import com.fortify.pub.bugtracker.support.Bug;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

public class Rally4PluginConnection {
	public static RallyRestApi restApi;
	static String rallyAPIKey;
	private static final Log LOG = LogFactory.getLog(Rally4PluginConnection.class);
	
	
	public Rally4PluginConnection(String rallyURL, String rallyAPI) {
		// TODO Auto-generated constructor stub
		if (rallyURL == null) {
			LOG.info("Cannot proceed without Rally API Key.");

		}
		try {
			restApi= new RallyRestApi(new URI(rallyURL),rallyAPI);
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			LOG.error("Cannot connect to rally with given API Key" +e);
		}
		restApi.setApplicationVersion("v2.0");
		restApi.setApplicationName("Rally Community");
	}
	
	public void closeRallyConnection() {
			try {
				restApi.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOG.error("Cannot close rally Connection" +e);
			}
		
	}
	
	
	
	
	
	public Bug createNewIssue(String projectKey, String workspace, String description, String id, String summary) throws IOException {

		JsonObject newDefect = new JsonObject();
		newDefect.addProperty("Severity", "4 - Minor");
		newDefect.addProperty("Description", description);
		newDefect.addProperty("Workspace", workspace);
		newDefect.addProperty("Project", projectKey);
		newDefect.addProperty("State", "Open");
		newDefect.addProperty("ConversationPost", id);
		CreateRequest createRequest = new CreateRequest("defect", newDefect);
		CreateResponse createResponse = restApi.create(createRequest);
	
		if(!createResponse.wasSuccessful()){
		LOG.error("Defect was not created");
		return null;
		}
		else{
		LOG.info("Created defect in Rally successfully");
	//	JsonObject rallyDefectObject = createResponse.getObject();
		String rallyDefectReference = createResponse.getObject().get("FormattedID").getAsString();
		LOG.info(String.format("Created %s", rallyDefectReference));
		final Bug retval = fetchDetails(rallyDefectReference,workspace);
        
		
		return retval;
	}
		
		
	}
	
	public Bug fetchDetails(String issueId, String workspace) throws IOException {  //DE...
		Bug retval = new Bug(issueId, "UNKNOWN");
		QueryRequest queryForDefect = new QueryRequest("Defect");
		queryForDefect.setFetch(new Fetch("FormattedID"));
		queryForDefect.setWorkspace(workspace);
		queryForDefect.setQueryFilter(new QueryFilter("FormattedID", "=", issueId)); 
        QueryResponse defectQueryResponse = restApi.query(queryForDefect);
        JsonObject defectRefObj = defectQueryResponse.getResults().get(0).getAsJsonObject();
        retval.setBugId(defectRefObj.get("FormattedID").toString());
		
		
		return retval;
	}
}



