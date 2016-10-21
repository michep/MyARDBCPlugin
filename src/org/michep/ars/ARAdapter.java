package org.michep.ars;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Field;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.arsys.api.QualifierInfo;
import com.bmc.arsys.api.QuerySourceValues;
import com.bmc.arsys.api.RegularQuery;
import com.bmc.arsys.pluginsvr.plugins.ARPluginContext;
import com.bmc.arsys.util.ARUtilEgcp;

public class ARAdapter {

	private ARServerUser ars = new ARServerUser();

	public ARAdapter(ARPluginContext ctx) throws ARException {
		String server = ctx.getARConfigEntry("Server-Connect-Name");
		if ((server == null) || (server.length() == 0))
			server = ctx.getARConfigEntry("Server-Name");
		String pass = ctx.getARConfigEntry("Remedy-App-Service-Password");
		pass = new ARUtilEgcp().GCEUtilApp(pass);
		ars = new ARServerUser("Remedy Application Service", pass, "", null, server);
		String port = ctx.getARConfigEntry("TCD-Specific-Port");
		int localport;
		if ((port != null) && (port.length() > 0)) {
			try {
				localport = Integer.parseInt(port);
				if (localport != 0)
					ars.setPort(localport);
			} catch (NumberFormatException e) {
				ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_ERROR, "Error converting parameter TCD-Specific-Port value " + port);
			}
		}
//		ars.setNativeAuthenticationInfo(ctx);
		ars.login();
	}

	public void impersonateUser(String userName) throws ARException {
		ars.impersonateUser(userName);
	}

	public List<Entry> getEntries(String form, String qualification, Map<Integer, String> fields) throws ARException {
		QualifierInfo qualInfo = ars.parseQualification(form, qualification);
		return getEntries(form, qualInfo, fields);
	}

	public List<Entry> getEntries(String form, QualifierInfo qualInfo, Map<Integer, String> fields) throws ARException {
		return ars.getListEntryObjects(form, qualInfo, 0, -1, null, set2array(fields.keySet()), false, null);
	}

	public String createEntry(String form, AREntry arentry) throws ARException {
		return ars.createEntry(form, arentry.getEntry());
	}

	public List<Field> getFields(String form, Map<Integer, String> fields) throws ARException {
		return ars.getListFieldObjects(form, set2array(fields.keySet()));
	}

	public List<QuerySourceValues> getListEntryObjects(RegularQuery query, int firstRetrieve, int maxRetrieve, boolean useLocale, OutputInteger nMatches) throws ARException {
		return ars.getListEntryObjects(query, firstRetrieve, maxRetrieve, useLocale, nMatches);
	}

	public void terminate() {
		ars.logout();
	}

	public List<Integer> getListField(String formName, int fieldType) throws ARException {
		return ars.getListField(formName, fieldType, 0);
	}
	
	public QualifierInfo parseQualification(String formName, String qual) throws ARException {
		return ars.parseQualification(formName, qual);
	}

	private int[] set2array(Set<Integer> set) {
		int size = set.size();
		int[] array = new int[size];
		int i = 0;
		for (Integer val : set) {
			array[i++] = val;
		}
		return array;
	}
}
