package com.fortify.pub.bugtracker.plugin;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class Rally4BugTrackerPlugin extends AbstractBugTrackerPlugin implements BatchBugTrackerPlugin{
	
	private static final Log LOG = LogFactory.getLog(Rally4BugTrackerPlugin.class);
	private Map<String, String> configValues = new HashMap<String, String>();
	
	
	//FetchDetails to be implemented ~ createDefect
	public Bug fetchBugDetails(String bugId, UserAuthenticationStore credentials) {
		Rally4PluginConnection connection = null;
		try {
			connection = getReusableConnection(credentials);
			final Bug bug = connection.fetchDetails(bugId);
			return bug;
		} catch (final RemoteException e) {
			LOG.info("Rally Error fetchBugDetails",e);
			throw new BugTrackerException("Rally Error fetchBugDetails", e);
		} finally {
			if (connection != null) {
				connection.closeRallyConnection();
			}
		}
	}

	public Bug fileBug(BugSubmission arg0, UserAuthenticationStore arg1) {
		// TODO Auto-generated method stub
	
		return null;
	}

	public String getBugDeepLink(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	//IsuueDetail has information on defect 

	public List<BugParam> getBugParameters(IssueDetail issueDetail, UserAuthenticationStore credentials) {
		final List<BugParam> initialFields = new ArrayList<BugParam>();
		Rally4PluginConnection connection = null;
		try {
			connection = getReusableConnection(credentials);

			BugParam summaryParam = new BugParamText()
					.setIdentifier("Defect_Summary")
					.setDisplayLabel("Defect Summary")
					.setRequired(true)
					.setDescription("Title of the Defect to be logged");
			if (issueDetail == null) {
				summaryParam = summaryParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
			} else {
				summaryParam = summaryParam.setValue(issueDetail.getSummary());
			}
			initialFields.add(summaryParam);

			BugParam descriptionParam = new BugParamTextArea()
					.setIdentifier("Defect_Description")
					.setDisplayLabel("Defect Description")
					.setRequired(true);
			if (issueDetail == null) {
				descriptionParam = descriptionParam.setValue("Issue Ids: $ATTRIBUTE_INSTANCE_ID$\n$ISSUE_DEEPLINK$");
			} else {
				descriptionParam.setValue(pluginHelper.buildDefaultBugDescription(issueDetail, true));
			}
			initialFields.add(descriptionParam);

			final BugParam project = new BugParamChoice()
					.setHasDependentParams(true)
					.setDisplayLabel("Project Key")
					.setDescription("Project Key")
					.setIdentifier("Rally_Project")
					.setRequired(true)
					.setValue(configValues.get("Rally_Project"));
			initialFields.add(project);

//			final BugParam priority = new BugParamChoice()
//					.setChoiceList(connection.getPriorityNames())
//					.setDisplayLabel("Priority")
//					.setIdentifier(PARAM_PRIORITY)
//					.setRequired(true);
//			initialFields.add(priority);

//			final StringBuilder dueInDescription = new StringBuilder("Optional timeframe for a fix within development. Can be adjusted within ");
//			dueInDescription.append(getShortDisplayName());
//			dueInDescription.append(" after filing.");
//			final BugParam dueIn = new BugParamChoice()
//					.setChoiceList(Arrays.asList("7 days", "14 days", "90 days", "180 days"))
//					.setDisplayLabel("Due In")
//					.setDescription(dueInDescription.toString())
//					.setIdentifier(PARAM_DUE_IN);
//			initialFields.add(dueIn);

//			BugParam assignee = new BugParamText()
//				.setDisplayLabel("Assignee")
//				.setIdentifier(PARAM_ASSIGNEE)
//				.setRequired(false);
//			if (issueDetail != null) {
//				assignee = assignee.setValue(issueDetail.getAssignedUsername());
//			}
//			initialFields.add(assignee);

//			if (configValues.get("Rally_Project") != null) {
//
//				final List<String> issueTypes = connection.getIssueTypes(project.getValue());
//				String defaultIssueType = configValues.get(JIRA_ISSUE_TYPE);
//
//				final BugParam issueType = new BugParamChoice()
//					.setChoiceList(issueTypes)
//					.setDisplayLabel("Issue Type")
//					.setIdentifier(JIRA_ISSUE_TYPE)
//					.setRequired(true);
//				if (issueTypes.contains(defaultIssueType)) {
//					issueType.setValue(defaultIssueType);
//				}
//				initialFields.add(issueType);
//
//				final List<String> versions = connection.getVersions(project.getValue());
//				final BugParam affectsVersion = new BugParamChoice()
//					.setChoiceList(versions)
//					.setDisplayLabel("Affects version")
//					.setIdentifier(PARAM_AFFECTS_VERSION);
//				initialFields.add(affectsVersion);
//			}


		} catch (final RemoteException e) {
			LOG.info("Rally Error getBugParameters",e);
			throw new BugTrackerException("Error in RallyBugTracker", e);
		} finally {
			if (connection != null) {
				connection.closeRallyConnection();
			}
		}

		return initialFields;
	}
	
	private Rally4PluginConnection getReusableConnection(UserAuthenticationStore credentials) throws RemoteException {
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
				.setRequired(false)
				.setValue("https://rally1.rallydev.com/slm/webservice/v2.0/workspace/37692205281");
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

	public Bug fileMultiIssueBug(MultiIssueBugSubmission arg0, UserAuthenticationStore arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<BugParam> getBatchBugParameters(UserAuthenticationStore arg0) {
		// TODO Auto-generated method stub
		return null;
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

	public List<BugParam> onBatchBugParameterChange(String arg0, List<BugParam> arg1, UserAuthenticationStore arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	public void reOpenBug(Bug arg0, String arg1, UserAuthenticationStore arg2) {
		// TODO Auto-generated method stub
		
	}


}
