package com.fortify.pub.bugtracker.plugin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.HashMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fortify.pub.bugtracker.support.Bug;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.Response;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.rallydev.rest.util.Ref;

public class Rally4PluginConnection {
	public static RallyRestApi restApi;
	static String rallyAPIKey;
	private static final Log LOG = LogFactory.getLog(Rally4PluginConnection.class);

	public String rallyDefectID;
	 public static JsonObject SCAObj;
	 String defaultTag = "SCA";
	
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
	
	public JsonObject addDefectToDefectSuite(JsonObject updateRallyDefectObject,String rWorkspace,String rProject, String rDSId) throws IOException {

		JsonObject defectSuiteJsonObject = null;
		JsonObject DSResponseObject = null;
		
		if (updateRallyDefectObject != null) {
			QueryRequest defectSuiteRequest = new QueryRequest("DefectSuite");
			defectSuiteRequest.setFetch(new Fetch("FormattedID", "Name", "Defects"));
			defectSuiteRequest.setWorkspace(rWorkspace);
			defectSuiteRequest.setProject(rProject);
			defectSuiteRequest.setQueryFilter(new QueryFilter("FormattedID", "=", rDSId));
			QueryResponse defectSuiteQueryResponse = restApi.query(defectSuiteRequest);
			
			if(defectSuiteQueryResponse.getTotalResultCount() != 0){
				defectSuiteJsonObject = defectSuiteQueryResponse.getResults().get(0).getAsJsonObject();
			}
		}
			if (defectSuiteJsonObject == null){
				LOG.error("Unable to resolve the defect suite details" + rDSId);
			return null;
		     }
		
		if (rDSId != null) {

			String defectRef = Ref.getRelativeRef(updateRallyDefectObject.get("_ref").getAsString());

			JsonObject defectSuitesOfThisDefect = (JsonObject) updateRallyDefectObject.get("DefectSuites");

			QueryRequest defectSuitesOfThisDefectRequest = new QueryRequest(defectSuitesOfThisDefect);
			JsonArray suites = restApi.query(defectSuitesOfThisDefectRequest).getResults();

			suites.add(defectSuiteJsonObject);

			JsonObject defectUpdate = new JsonObject();
			defectUpdate.add("DefectSuites", suites);
			UpdateRequest updateDefectRequest = new UpdateRequest(defectRef, defectUpdate);
			UpdateResponse updateResponse = restApi.update(updateDefectRequest);
			
			 DSResponseObject = updateResponse.getObject().getAsJsonObject();
			
			 createErrorReport(updateResponse, "DefectSuite");
		}
		return defectSuiteJsonObject;
		
	}

	
	public Bug createNewIssue(String rProject, String rWorkspace, String rDSId, String rAPI, String rURL, String instanceId, 
			String description, String issueType, String fortifySeverity, String issueDeepLink) throws IOException, JAXBException, URISyntaxException {
		
		if (rURL == null) {
			LOG.info("Cannot proceed without Rally API Key.");

		}
		try {
			LOG.info("Connection ToTry");
			restApi= new RallyRestApi(new URI(rURL),rAPI);
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			LOG.error("Cannot connect to rally with given API Key" +e);
		}
		restApi.setApplicationVersion("v2.0");
		restApi.setApplicationName("Rally Community");
		
		String rallySeverity = null;
		JsonArray tagsArray;
		 
		String[] descrptionP = description.split("]");
		//String newLine = System.lineSeparator();
		String str = "";
		for(int i=0; i <descrptionP.length; i++){
			str += descrptionP[i]+"<br><br>";
		}
		LOG.error("This is fortifyseverity" +fortifySeverity);
		if(fortifySeverity.equalsIgnoreCase("[Critical]")){
			rallySeverity = "1 - Catastrophic";
		}
		else if(fortifySeverity.equalsIgnoreCase("[High]")){
			rallySeverity = "2 - Severe";
		}
		else if(fortifySeverity.equalsIgnoreCase("[Medium]")){
			rallySeverity = "3 - Non - Critical";
		}
		else if(fortifySeverity.equalsIgnoreCase("[Low]") ||fortifySeverity.equalsIgnoreCase("[Information]") ){
			rallySeverity = "4 - Minor";
		}
		else {
			rallySeverity = "";
		}
		
		JsonObject newDefect = new JsonObject();
		newDefect.addProperty("Name", issueType.replace("[", "").replace("]","")+defaultTag);
		
		newDefect.addProperty("Severity", rallySeverity);
		newDefect.addProperty("Description", str.replace("[", "").replace("]",""));
		newDefect.addProperty("Workspace", rWorkspace);
		newDefect.addProperty("Project", rProject);
		newDefect.addProperty("State", "Open");
		newDefect.addProperty("ConversationPost", instanceId);
		CreateRequest createRequest = new CreateRequest("defect", newDefect);
		CreateResponse createResponse = null;
		try {
			createResponse = restApi.create(createRequest);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOG.error("create Response failed due to following Exception" +e);
		}
	
		if(!createResponse.wasSuccessful()){
		LOG.error("Defect was not created");
		return null;
		}
		else{
		LOG.error("Created defect in Rally successfully");
		String rallyDefectReference = createResponse.getObject().get("FormattedID").getAsString();
        JsonObject defectObj = createResponse.getObject();
		 String fullDefectAPIURL=createResponse.getObject().get("_ref").getAsString();
         String [] tmp1=fullDefectAPIURL.split("/");
         int tmplen=tmp1.length;
         rallyDefectID=tmp1[tmplen-1];
		
		JsonObject tagRefObject = tagExists(defaultTag,rWorkspace, rProject );
	      if(tagRefObject == null){
		 SCAObj = createTag(defaultTag, rWorkspace, rProject);
	      }
	      else{
	    	  SCAObj = tagRefObject;
	      }
		addIdToDisc(defectObj,rWorkspace,issueDeepLink);
		tagsArray = new JsonArray();
		tagsArray.add(SCAObj);
		JsonObject updateRallyDefectObject = updateDefectWithTags(tagsArray, defectObj);
		JsonObject defectsuiteObj = addDefectToDefectSuite(updateRallyDefectObject, rWorkspace,rProject,rDSId);
		Bug retval = fetchDetails(rallyDefectReference,rWorkspace); 
        
		return retval;
	}
		
		
	}
	
public void addIdToDisc(JsonObject defectObj,String rWorkspace, String issueDeepLink) throws IOException, JAXBException, URISyntaxException{
		
		QueryRequest queryForDefect = new QueryRequest("Defect");
		queryForDefect.setFetch(new Fetch("FormattedID"));
		queryForDefect.setWorkspace(rWorkspace);
		queryForDefect.setQueryFilter(new QueryFilter("FormattedID", "=", defectObj.get("FormattedID").getAsString())); 
        QueryResponse defectQueryResponse = restApi.query(queryForDefect);
        JsonObject defectRefObj = defectQueryResponse.getResults().get(0).getAsJsonObject();
        
        System.err.println("This is defectRefObj" +defectRefObj.toString());
        if(defectQueryResponse.getTotalResultCount() == 0)
        {
        	LOG.error("Defect doesnot exist");
        }
        else
        	LOG.info("Defect exists now add discussion item to this");
            		issueDeepLink = issueDeepLink.replace("[", "").replace("]","");
            		String   url = "<a href=" + issueDeepLink + ">" +issueDeepLink+ "</a>";
    	        JsonObject newConv = new JsonObject();           
                newConv.addProperty("Text", url);
                newConv.addProperty("Artifact",defectRefObj.get("_ref").getAsString());
                newConv.addProperty("PostNumber", 1);
                CreateRequest createRequest = new CreateRequest("ConversationPost", newConv);
                CreateResponse createResponse = restApi.create(createRequest);
                createErrorReport(createResponse, "Discussion");
        
         String fullDefectAPIURL=defectRefObj.get("_ref").getAsString();
         String [] tmp1=fullDefectAPIURL.split("/");
         int tmplen=tmp1.length;
         String rallyDefectID=tmp1[tmplen-1];
         
         LOG.info("Added comment in rally successfully");
        		         
	}

public JsonObject tagExists(String tagName, String rallyWorkspace, String rallyProject) throws IOException{
	
	JsonObject tagRefObject = null;
	QueryRequest tagRequest = new QueryRequest("Tag");
	tagRequest.setFetch(new Fetch("Name"));
	tagRequest.setWorkspace(rallyWorkspace);
	tagRequest.setProject(rallyProject);
	
	tagRequest.setQueryFilter(new QueryFilter("Name", "=", tagName));
	QueryResponse tagResponse = restApi.query(tagRequest);
	
	if (tagResponse.getTotalResultCount() != 0){ //do count here
		LOG.info("tag " + tagName + " already exists");
		tagRefObject = tagResponse.getResults().get(0).getAsJsonObject();
		return tagRefObject;
	}
	else{
		LOG.info(tagName +" doesnot exist,creating tag");
		return null;
		}
}


//Creates a tag with given name, returns tag reference
public JsonObject createTag(String tagName, String rallyWorkspace, String rallyProject) throws IOException {
	String objType = "Tag";
	JsonObject tagRefObject = null;
	JsonObject newTag = new JsonObject();
	newTag.addProperty("Name", tagName);
	newTag.addProperty("Workspace", rallyWorkspace);
	newTag.addProperty("Project", rallyProject);
	CreateRequest createRequestTag = new CreateRequest("tag", newTag);

	CreateResponse createResponseTag = restApi.create(createRequestTag);
			if(!createResponseTag.wasSuccessful()){
					createErrorReport(createResponseTag, objType);
					}
			else{
					tagRefObject = createResponseTag.getObject();
					String tagReference = createResponseTag.getObject().get("_ref").getAsString();
					LOG.info(String.format("Created %s", tagReference));
					return tagRefObject;
				}
	return tagRefObject;
}

public JsonObject updateDefectWithTags(JsonArray tagArray, JsonObject rallyDefectObject)
		throws IOException {
	
	JsonObject defectUpdateTag = new JsonObject();
	JsonObject updateRallyDefectObject = new JsonObject();
	defectUpdateTag.add("Tags", tagArray);

	String rallyDefectReference = rallyDefectObject.get("_ref").getAsString();
	UpdateRequest updateDefectRequestTag = new UpdateRequest(rallyDefectReference, defectUpdateTag);
	UpdateResponse updateResponseTag = restApi.update(updateDefectRequestTag);
	updateRallyDefectObject = updateResponseTag.getObject();
	
	
	createErrorReport(updateResponseTag, "Tag");
	
	return updateRallyDefectObject;
}
	public Bug fetchDetails(String issueId, String workspace) throws IOException {
		     
		//String issueId = defectsuiteObj.get("FormattedID").getAsString();		 
		Bug retval = new Bug(issueId, "UNKNOWN");
		QueryRequest queryForDefect = new QueryRequest("Defect");
		queryForDefect.setFetch(new Fetch("FormattedID"));
		queryForDefect.setWorkspace(workspace);
		queryForDefect.setQueryFilter(new QueryFilter("FormattedID", "=", issueId)); 
        QueryResponse defectQueryResponse = restApi.query(queryForDefect);
        JsonObject defectRefObj = defectQueryResponse.getResults().get(0).getAsJsonObject();
        retval.setBugId(defectRefObj.get("_ref").toString());
		
		
		return retval;
	}
	
	//Check if the error responses/messages are relevant enough
		public void createErrorReport(Response response, String objType) {
			if (!response.wasSuccessful()) {
				LOG.error("Could not create " + objType + " successfully with the below description: \n");
				for (String eachWarning : response.getWarnings()) {
					if (eachWarning != null)
						LOG.error("Warning: " + eachWarning);
				}
				for (String eachError : response.getErrors()) {
					if (eachError != null)
						LOG.error("Error: " + eachError);
				}

			} else {
				if (LOG.isDebugEnabled()) {
					for (String eachWarning : response.getWarnings()) {
						if (eachWarning != null)
							LOG.error("Warning: " + eachWarning);
					}
				}
			}
		}
}



