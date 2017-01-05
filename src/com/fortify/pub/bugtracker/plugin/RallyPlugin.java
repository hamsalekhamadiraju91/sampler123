package com.fortify.pub.bugtracker.plugin;



import static com.fortify.pub.bugtracker.support.BugTrackerPluginConstants.DISPLAY_ONLY_SUPPORTED_VERSION;
import static com.fortify.sample.bugtracker.bugzilla.BugzillaPluginConstants.BUGZILLA_URL_NAME;
import static com.fortify.sample.bugtracker.bugzilla.BugzillaPluginConstants.SUPPORTED_VERSIONS;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fortify.pub.bugtracker.plugin.BugTrackerPlugin;
import com.fortify.pub.bugtracker.support.Bug;
import com.fortify.pub.bugtracker.support.BugParam;
import com.fortify.pub.bugtracker.support.BugParamText;
import com.fortify.pub.bugtracker.support.BugParamTextArea;
import com.fortify.pub.bugtracker.support.BugSubmission;
import com.fortify.pub.bugtracker.support.BugTrackerAuthenticationException;
import com.fortify.pub.bugtracker.support.BugTrackerConfig;
import com.fortify.pub.bugtracker.support.BugTrackerException;
import com.fortify.pub.bugtracker.support.IssueDetail;
import com.fortify.pub.bugtracker.support.UserAuthenticationStore;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;


@BugTrackerPluginImplementation
public class RallyPlugin extends AbstractBugTrackerPlugin implements BugTrackerPlugin {
	public static RallyRestApi restApi ;
	static String rallyAPIKey;
	public static final String RALLY_URL = "https://rally1.rallydev.com";

	public Bug fetchBugDetails(String arg0, UserAuthenticationStore rallyCredentials) {
	    return null;
	}

	public Bug fileBug(BugSubmission arg0, UserAuthenticationStore rallyCredentials) {

		return null;
	}

	private URL rallyURL;
	public String getBugDeepLink(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public List<BugParam> getBugParameters(IssueDetail issueDetail, UserAuthenticationStore rallyCredentials) {
		connectToRally();
		final BugParam descriptionParam = getDescriptionText(issueDetail);
		final BugParam summaryParam = getSummaryParamText(issueDetail);
		final BugParam issueIdParam = getIssueIdParam(issueDetail);
		final BugParam issueLineNoParam= getIssueLineNoParam(issueDetail);
		

		return Arrays.asList(summaryParam, descriptionParam,issueIdParam,issueLineNoParam);
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
//		final BugTrackerConfig supportedVersions = new BugTrackerConfig()
//                .setIdentifier(DISPLAY_ONLY_SUPPORTED_VERSION)
//                .setDisplayLabel("Supported Versions")
//                .setDescription("Bug Tracker versions supported by the plugin")
//                .setValue(SUPPORTED_VERSIONS)
//                .setRequired(false);

		BugTrackerConfig bugTrackerConfig = new BugTrackerConfig()
				.setIdentifier("Rally Plugin")
				.setDisplayLabel("Rally URL Prefix")
				.setDescription("Rally URL prefix")
				.setRequired(true);

		List<BugTrackerConfig> configs = Arrays.asList(supportedVersions, bugTrackerConfig);
		pluginHelper.populateWithDefaultsIfAvailable(configs);
		return configs;
	}


	public String getLongDisplayName() {
		// TODO Auto-generated method stub
		return "File Bug in Rally";
	}


	public String getShortDisplayName() {
		// TODO Auto-generated method stub
		return "Rally Plugin For SSC";
	}

	
	public List<BugParam> onParameterChange(IssueDetail arg0, String arg1, List<BugParam> arg2,
			UserAuthenticationStore arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public boolean requiresAuthentication() {
		// TODO Auto-generated method stub
		return true;
	}

	
	public void setConfiguration(Map<String, String> config) throws BugTrackerAuthenticationException {
		// TODO Auto-generated method stub
		
		if (config.get(RALLY_URL) == null) {
			throw new IllegalArgumentException("Invalid configuration passed");
		}

		String url = config.get(RALLY_URL);
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			throw new BugTrackerException("Rally URL protocol should be either http or https");
		}

		if (url.endsWith("/")) {
			url = url.substring(0,url.length()-1);
		}

		try {
			rallyURL = new URL(url);
			rallyURL.toURI();
			if (rallyURL.getHost().length() == 0) {
				throw new BugTrackerException("Rally URL host should not be empty");
			}
		} catch (URISyntaxException e) {
			throw new BugTrackerException("Invalid Rally URL: " + url);
		} catch (MalformedURLException e) {
			throw new BugTrackerException("Invalid Rally URL: " + url);
		}
	
		

	}

	
	public void testConfiguration(UserAuthenticationStore rallyCredentials) {
		validateCredentials(rallyCredentials);
		
	}

	public void validateCredentials(UserAuthenticationStore rallyCredentials)  {
		connectToRally();
	}

	private void connectToRally() {
		// TODO Auto-generated method stub
		if (rallyAPIKey == null) {
			System.out.println("Cannot proceed without Rally API Key.");
		}
		try {
			restApi= new RallyRestApi(new URI(RALLY_URL), rallyAPIKey);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		restApi.setApplicationVersion("v2.0");
		restApi.setApplicationName("Rally Community");
		
	}

}
