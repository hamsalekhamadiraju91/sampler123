package com.fortify.pub.bugtracker.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import com.fortify.pub.bugtracker.support.Bug;
import com.fortify.pub.bugtracker.support.BugParam;
import com.fortify.pub.bugtracker.support.BugParamChoice;
import com.fortify.pub.bugtracker.support.BugParamText;
import com.fortify.pub.bugtracker.support.BugParamTextArea;
import com.fortify.pub.bugtracker.support.BugSubmission;
import com.fortify.pub.bugtracker.support.BugTrackerAuthenticationException;
import com.fortify.pub.bugtracker.support.BugTrackerConfig;
import com.fortify.pub.bugtracker.support.BugTrackerException;
import com.fortify.pub.bugtracker.support.IssueDetail;
import com.fortify.pub.bugtracker.support.MultiIssueBugSubmission;
import com.fortify.pub.bugtracker.support.UserAuthenticationStore;


@BugTrackerPluginImplementation
public  class Rally4BugTrackerPlugin extends AbstractBugTrackerPlugin implements BatchBugTrackerPlugin{
	
	private static final Log LOG = LogFactory.getLog(Rally4BugTrackerPlugin.class);
	private static final String PARAM_SUMMARY = "summary";
	private static final String PARAM_DESCRIPTION = "description";
	private static final String INSTANCE_ID = "defectId";
	private static final String RALLY_WORKSPACE = "https://rally1.rallydev.com/slm/webservice/v2.0/workspace/37692205281" ;
	private Map<String, String> configValues = new HashMap<String, String>();
	
	String instanceId;
	String issueType;
	
	//FetchDetails to be implemented ~ createDefect
	public Bug fetchBugDetails(String bugId, UserAuthenticationStore credentials) {
		Rally4PluginConnection connection = null;
		try {
			connection = getReusableConnection(credentials);
			Bug bug;
			try {
				bug = connection.fetchDetails(bugId,RALLY_WORKSPACE);
				return bug;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOG.error(e);
			}
			
		} catch (final Exception e) {
			LOG.info("Rally Error fetchBugDetails",e);
			throw new BugTrackerException("Rally Error fetchBugDetails", e);
			
		} finally {
			if (connection != null) {
				connection.closeRallyConnection();
			}
		}
		return null;
	}

	public Bug fileBug(BugSubmission bug, UserAuthenticationStore credentials) {
	//	IssueDetail issue = bug.getIssueDetail();
	//	instanceId = issue.getIssueInstanceId();
		issueType = "Name of the issue";
		
		return fileBug(bug.getParams(), credentials);
				
			
	}

	private Bug fileBug(Map<String, String> params, UserAuthenticationStore credentials) {
		
		Bug retval = null;
		Rally4PluginConnection connection = null;
		
			connection = getReusableConnection(credentials);
            try {
            	LOG.error("This is before createNewIssue---------------------------->" +params.values());
				retval = connection.createNewIssue(configValues.get("Rally_Project"),configValues.get("Rally_WorkSpace"), configValues.get("Rally_DefectSuite"),
						configValues.get("Rally_API"), configValues.get("Rally_URL"), INSTANCE_ID, params.get(PARAM_DESCRIPTION), "Issue Title");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOG.error("This is logs error for fileBug" +configValues.get("Rally_Workspace"));
			}
			if (connection != null) {
				connection.closeRallyConnection();
			}
			return retval;
		}
		
	public String getBugDeepLink(String issueID) {
		return configValues.get("Rally_URL");
	}
	
	
	//IsuueDetail has information on defect 


	public List<BugParam> getBugParameters(IssueDetail issueDetail, UserAuthenticationStore credentials) {
		final List<BugParam> initialFields = new ArrayList<BugParam>();
		Rally4PluginConnection connection = null;
		try {
			connection = getReusableConnection(credentials);

			BugParam summaryParam = new BugParamText()
					.setIdentifier(PARAM_SUMMARY)
					.setDisplayLabel("Bug Summary")
					.setRequired(true)
					.setDescription("Title of the bug to be logged");
			if (issueDetail == null) {
				summaryParam = summaryParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
			} else {
				summaryParam = summaryParam.setValue(issueDetail.getSummary());
			}
			initialFields.add(summaryParam);

			BugParam descriptionParam = new BugParamTextArea()
					.setIdentifier(PARAM_DESCRIPTION)
					.setDisplayLabel("Bug Description")
					.setRequired(true);
			if (issueDetail == null) {
				descriptionParam = descriptionParam.setValue("Issue Ids: $ATTRIBUTE_INSTANCE_ID$\n$ISSUE_DEEPLINK$");
			} else {
				descriptionParam.setValue(issueDetail.getDescription());
			}
			initialFields.add(descriptionParam);
			
			BugParam instanceID = new BugParamText()
					.setIdentifier(INSTANCE_ID)
					.setDisplayLabel("Instance ID")
					.setRequired(true)
					.setDescription("Issue unique Instance ID");
			if (issueDetail == null) {
				instanceID = instanceID.setValue("$ATTRIBUTE_INSTANCE_ID");
			} else {
				instanceID = instanceID.setValue("$ATTRIBUTE_INSTANCE_ID");
			}
			initialFields.add(instanceID);
			
			BugParam rallyWorkspace = new BugParamText()
					.setIdentifier("Rally_Workspace")
					.setDescription("Rally Workspace")
					.setRequired(true)
					.setDisplayLabel("Rally Workspace")
					.setValue(configValues.get("Rally_WorkSpace"));
			initialFields.add(rallyWorkspace);
			
			BugParam rallyProject = new BugParamText()
					.setIdentifier("Rally_Project")
					.setDescription("Rally Project")
					.setRequired(true)
					.setDisplayLabel("Rally Project")
			        .setValue(configValues.get("Rally_Project"));
			initialFields.add(rallyProject);
			
			BugParam rallyDS = new BugParamText()
					.setIdentifier("Rally_DefectSuite")
					.setDescription("Rally Defect Suite ID")
					.setRequired(true)
					.setDisplayLabel("Rally DefectSuite")
			        .setValue(configValues.get("Rally_DefectSuite"));
			initialFields.add(rallyDS);
 
 

		} catch (final Exception e) {
			LOG.info("Rally Error getBugParameters",e);
			throw new BugTrackerException("Error in RallyBugTracker", e);
		} finally {
			if (connection != null) {
				connection.closeRallyConnection();
			}
		}

		return initialFields;
	}
	
	private Rally4PluginConnection getReusableConnection(UserAuthenticationStore credentials) {
		try {
			return new Rally4PluginConnection(configValues.get("Rally_URL"), configValues.get("Rally_API"));
		} catch (Exception e) {
			LOG.error("Rally Error getConnection",e);
			throw new BugTrackerAuthenticationException("Error in connection", e);
		}
	}

	public List<BugTrackerConfig> getConfiguration() {
		BugTrackerConfig rallyURLConfig = new BugTrackerConfig()
				.setIdentifier("Rally_URL")
				.setDisplayLabel("Rally URL Prefix")
				.setDescription("Rally URL prefix")
				.setRequired(true);
		BugTrackerConfig rallyAPIConfig = new BugTrackerConfig()
				.setIdentifier("Rally_API")
				.setDescription("Api key")
				.setDisplayLabel("API key")
				.setRequired(true);
		BugTrackerConfig rallyDefectSuite = new BugTrackerConfig()
				.setIdentifier("Rally_DefectSuite")
				.setDisplayLabel("Rally Defect Suite ID")
				.setDescription("Rally DefectSuite")
				.setRequired(true);
		BugTrackerConfig rallyWorkSpace = new BugTrackerConfig()
				.setDescription("Rally WorkSpace")
				.setDisplayLabel("Rally WorkSpace")
				.setIdentifier("Rally_WorkSpace")
				.setRequired(true);
				
		BugTrackerConfig rallyProject = new BugTrackerConfig()
				.setDescription("Rally Project")
				.setDisplayLabel("Rally Project")
				.setIdentifier("Rally_Project")
				.setRequired(true);
		
		List<BugTrackerConfig> rallyConfigs = Arrays.asList(rallyURLConfig,rallyAPIConfig,rallyDefectSuite,rallyWorkSpace,rallyProject);
		pluginHelper.populateWithDefaultsIfAvailable(rallyConfigs);
		return rallyConfigs;
	}

	public String getLongDisplayName() {
		// TODO Auto-generated method stub
		final StringBuilder sb = new StringBuilder(getShortDisplayName());
		sb.append(" (");
		sb.append(configValues.get("Rally_URL"));
		sb.append(')');
		return sb.toString();
	}

	public String getShortDisplayName() {
		// TODO Auto-generated method stub
		return "Rally Plugin";
	}

	public List<BugParam> onParameterChange(IssueDetail arg0, String arg1, List<BugParam> arg2,
			UserAuthenticationStore arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean requiresAuthentication() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setConfiguration(Map<String, String> configuration) {
		configValues = configuration;
		
		String url = configuration.get("Rally_URL");
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			throw new BugTrackerException("Rally URL protocol should be either http or https");
		}
		
		if (url.endsWith("/")) {
			url = url.substring(0,url.length()-1);
			configuration.put("Rally_URL", url);
		}

		try {
			URL urltrue = new URL(configValues.get("Rally_URL"));
			urltrue.toURI();
			if (urltrue.getHost().length() == 0) {
				throw new BugTrackerException("Rally host cannot be empty");
			}
		} catch (MalformedURLException e)
		{
			throw new BugTrackerException("Invalid Rally URL: " + configValues.get("Rally_URL"));
		}
		catch (URISyntaxException e) {
			throw new BugTrackerException("Invalid Rally URL: " + configValues.get("Rally_URL"));
		}
		
	}

	//This has to originally test all the user given credentials exhaustively as in JIRA
	public void testConfiguration(UserAuthenticationStore arg0) {
		// TODO Auto-generated method stub
		Rally4PluginConnection connection = null;
		connection = new Rally4PluginConnection(configValues.get("Rally_URL"), configValues.get("Rally_API"));
		connection.closeRallyConnection();
	}

	public void validateCredentials(UserAuthenticationStore arg0) {
		// TODO Auto-generated method stub
		
	}

	public void addCommentToBug(Bug arg0, String arg1, UserAuthenticationStore arg2) {
		// TODO Auto-generated method stub
		
	}

	public Bug fileMultiIssueBug(MultiIssueBugSubmission bugs, UserAuthenticationStore credentials) {
		return fileBug(bugs.getParams(),credentials);
////		List<IssueDetail> issueList = new ArrayList<IssueDetail>();
////		issueList = bugs.getIssueDetails();
////		Bug b = null;
////		for(IssueDetail eachIssue: issueList){
////			try {
////				b = fileBug(eachIssue, credentials);
////			} catch (IOException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}	
////		}
//		Map<String, String> issuesMap;
//		issuesMap = bugs.getParams();
//		Bug b = null;
//	    try {
//			b = fileBug(issuesMap,credentials);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		return b;
			
	}

	public List<BugParam> getBatchBugParameters(UserAuthenticationStore credentials) {
		// TODO Auto-generated method stub
		return getBugParameters(null, credentials);
	}

	public boolean isBugClosed(Bug arg0, UserAuthenticationStore arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isBugClosedAndCanReOpen(Bug arg0, UserAuthenticationStore arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isBugOpen(Bug arg0, UserAuthenticationStore arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	public List<BugParam> onBatchBugParameterChange(String arg0, List<BugParam> arg1, UserAuthenticationStore credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	public void reOpenBug(Bug arg0, String arg1, UserAuthenticationStore arg2) {
		// TODO Auto-generated method stub
		
	}


}
