package com.fortify.pub.bugtracker.plugin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fortify.pub.bugtracker.support.Bug;
import com.rallydev.rest.RallyRestApi;

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
	
	public Bug fetchDetails(String issueId) {
		return null;
	}

}
