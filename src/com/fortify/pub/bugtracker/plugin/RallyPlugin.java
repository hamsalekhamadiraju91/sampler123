package com.fortify.pub.bugtracker.plugin;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fortify.pub.bugtracker.plugin.BugTrackerPlugin;
import com.fortify.pub.bugtracker.support.Bug;
import com.fortify.pub.bugtracker.support.BugParam;
import com.fortify.pub.bugtracker.support.BugParamText;

import com.fortify.pub.bugtracker.support.BugSubmission;
import com.fortify.pub.bugtracker.support.BugTrackerAuthenticationException;
import com.fortify.pub.bugtracker.support.BugTrackerConfig;
import com.fortify.pub.bugtracker.support.BugTrackerException;
import com.fortify.pub.bugtracker.support.IssueDetail;
import com.fortify.pub.bugtracker.support.UserAuthenticationStore;
import com.j2bugzilla.base.BugFactory;
import com.j2bugzilla.rpc.GetBug;
import com.j2bugzilla.rpc.ReportBug;
import com.rallydev.rest.RallyRestApi;


@BugTrackerPluginImplementation
public class RallyPlugin extends AbstractBugTrackerPlugin implements BugTrackerPlugin {
	public static RallyRestApi restApi ;
	static String rallyAPIKey;
	public static final String RALLY_URL = "https://rally1.rallydev.com";
    String bugDeepLink;
    
    
    
    //implement according to Rally plugin
    
	public Bug fetchBugDetails(String bugID, UserAuthenticationStore rallyCredentials) {
		final GetBug getBug = new GetBug(Integer.parseInt(bugID));
		try{
		com.j2bugzilla.base.Bug dtoBug = getBug.getBug();
		return new Bug(String.valueOf(dtoBug.getID()), dtoBug.getStatus(), dtoBug.getResolution());
	} catch (Exception e) {
		throw new BugTrackerException("The bug status could not be fetched correctly", e);
	}
	}

	public Bug fileBug(BugSubmission bug, UserAuthenticationStore rallyCredentials) {
		BugFactory factory = new BugFactory();
		com.j2bugzilla.base.Bug dtoBug = factory.createBug(getPostDataStrMap(bug.getParams()));
		final ReportBug reportBug = new ReportBug(dtoBug);
		
		
           Bug b =new Bug("Bug1","NEW");
            return b;
	}
	

	private Map<String, Object> getPostDataStrMap(Map<String, String> paramList) {
		final Map<String, Object> result = new HashMap<String, Object>();
		for (Map.Entry<String, String> p : paramList.entrySet()) {
			result.put(p.getKey(), p.getValue());
		}
		result.put("comment", paramList.get("description"));
        String summary = paramList.get("summary");
        if (summary != null && summary.length() > 255) {
            summary = summary.substring(0, 255);
        }
		result.put("short_desc", summary);
		return result;
	}


	private URL rallyURL;
	private String apikey;
	
	
	//Not implemented
	public String getBugDeepLink(String arg0) {   
		bugDeepLink = RALLY_URL;
		return bugDeepLink;
	}

	
	public List<BugParam> getBugParameters(IssueDetail issueDetail, UserAuthenticationStore rallyCredentials) {
		//connectToRally();
		final BugParam descriptionParam = getDescriptionText(issueDetail);
		final BugParam summaryParam = getSummaryParamText(issueDetail);
		final BugParam issueIdParam = getIssueIdParam(issueDetail);
		final BugParam issueLineNoParam= getIssueLineNoParam(issueDetail);
		final BugParam projectNameParam= getProjectNameParam(issueDetail);
	

		return Arrays.asList(summaryParam, descriptionParam,issueIdParam,issueLineNoParam,projectNameParam);
	}
	
	
	private BugParam getProjectNameParam(IssueDetail issueDetail) {
		// TODO Auto-generated method stub
		return null;
	}

	private BugParam getIssueLineNoParam(IssueDetail issueDetail) {
		final BugParam issueLineNoParam = new BugParamText()
				.setIdentifier("Issue Line Number")
				.setDisplayLabel("Issue LineNo.")
				.setRequired(true)
				.setDescription("Issue Line Number");
		if (issueDetail == null) {
			issueLineNoParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
		} else {
			issueLineNoParam.setValue(issueDetail.getLineNumber().toString());
		}
		return issueLineNoParam;
		
	}

	private BugParam getIssueIdParam(IssueDetail issueDetail) {
		final BugParam issueIdParam = new BugParamText()
				.setIdentifier("Issue ID")
				.setDisplayLabel("Issue ID")
				.setRequired(true)
				.setDescription("Issue Instance ID");
		if (issueDetail == null) {
			issueIdParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
		} else {
			issueIdParam.setValue(issueDetail.getIssueInstanceId());
		}
		return issueIdParam;
	}

	private BugParam getSummaryParamText(final IssueDetail issueDetail) {
		final BugParam summaryParam = new BugParamText()
				.setIdentifier("Summary")
				.setDisplayLabel("Issue Summary")
				.setRequired(true)
				.setDescription("Title of the Issue in Rally");
		if (issueDetail == null) {
			summaryParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
		} else {
			summaryParam.setValue(issueDetail.getSummary());
		}
		return summaryParam;
	}
	
	
	private BugParam getDescriptionText(final IssueDetail issueDetail) {
		final BugParam descriptionParam = new BugParamText()
				.setIdentifier("Issue Description")
				.setDisplayLabel("IssueDescription")
				.setRequired(true)
				.setDescription("Issue Description");
		if (issueDetail == null) {
			descriptionParam.setValue("Issue Ids: $ATTRIBUTE_INSTANCE_ID$\n$ISSUE_DEEPLINK$");
		} else {
			descriptionParam.setValue(pluginHelper.buildDefaultBugDescription(issueDetail, true));
		}
		return descriptionParam;
	}
	
	public List<BugTrackerConfig> getConfiguration() {
//		List<BugTrackerConfig> configs = Arrays.asList();
		BugTrackerConfig bugTrackerConfig = new BugTrackerConfig()
				.setIdentifier("RallyURL")
				.setDisplayLabel("Rally URL Prefix")
				.setDescription("Rally URL prefix")
				.setRequired(true);
		BugTrackerConfig bugTrackerConfig1 = new BugTrackerConfig()
				.setIdentifier("RallyAPI")
				.setDescription("Api key")
				.setDisplayLabel("API key")
				.setRequired(true);
	

		List<BugTrackerConfig> configs = Arrays.asList(bugTrackerConfig,bugTrackerConfig1);
	//	pluginHelper.populateWithDefaultsIfAvailable(configs);
		return configs;
	}


	public String getLongDisplayName() {
		// TODO Auto-generated method stub
		return "File Bug in Rally";
	}


	public String getShortDisplayName() {
		// TODO Auto-generated method stub
		return "Rally_Sample";
	}

	
	public List<BugParam> onParameterChange(IssueDetail issueDetail, String changedParamIdentifier, List<BugParam> currentValues,
			UserAuthenticationStore rallyCredentials) {
		// TODO Auto-generated method stub
		return currentValues;
	}

	
	public boolean requiresAuthentication() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public void setConfiguration(Map<String, String> config) throws BugTrackerAuthenticationException {
		// TODO Auto-generated method stub
		if (config.get("RallyAPI") == null) {
			System.out.println("Cannot proceed without Rally API Key.");
		}
		try {
			restApi= new RallyRestApi(new URI(config.get("RallyURL")), config.get("RallyAPI"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		restApi.setApplicationVersion("v2.0");
		restApi.setApplicationName("Rally Community");
	
		

	}

	
	public void testConfiguration(UserAuthenticationStore rallyCredentials) {
//		validateCredentials(rallyCredentials);
		
	}

	public void validateCredentials(UserAuthenticationStore rallyCredentials)  {
//		connectToRally();
	}

//	private void connectToRally() {
//		// TODO Auto-generated method stub
//		if (rallyAPIKey == null) {
//			System.out.println("Cannot proceed without Rally API Key.");
//		}
//		try {
//			restApi= new RallyRestApi(new URI(RALLY_URL), rallyAPIKey);
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		restApi.setApplicationVersion("v2.0");
//		restApi.setApplicationName("Rally Community");
//		
//	}

}
