package org.michep.ardbc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.michep.ars.ARAdapter;
import org.michep.ars.AREntry;
import org.michep.ars.ARForm;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.QualifierInfo;
import com.bmc.arsys.pluginsvr.plugins.ARPluginContext;

public class Config {

	private ARForm formConfig;
	private ARForm formQuery;
	private List<String> configFieldNames = Arrays.asList("Primary Form Name", "Primary Form Relation FieldID", "Subquery Operation", "Subquery Form Name", "Subquery Form Relation FieldID");
//	private List<String> queryFieldNames = Arrays.asList("Primary Form Qual", "Subquery Form Qual");

	public Map<String, Map<String, String>> config = new HashMap<String, Map<String, String>>();

	public Config(ARPluginContext ctx) throws ARException {
		formConfig = createConfigForm(ctx);
		formQuery = createQueryForm(ctx);
		loadConfig();
	}

	public Map<String, String> getConfigOptions(String configName) {
		return config.get(configName);
	}

	public Set<String> getConfigNames() {
		return config.keySet();
	}

	private ARForm createConfigForm(ARPluginContext ctx) throws ARException {
		Map<Integer, String> fields = new HashMap<>();
		fields.put(8, "Name");
		fields.put(400000000, "Primary Form Name");
		fields.put(400000002, "Primary Form Relation FieldID");
		fields.put(400000003, "Subquery Operation");
		fields.put(400000004, "Subquery Form Name");
		fields.put(400000005, "Subquery Form Relation FieldID");
		return new ARForm(new ARAdapter(ctx), "ARDBC:Config", fields);
	}

	private ARForm createQueryForm(ARPluginContext ctx) throws ARException {
		Map<Integer, String> fields = new HashMap<>();
		fields.put(8, "Query GUID");
		fields.put(400000100, "Primary Form Qual");
		fields.put(400000200, "Subquery Form Qual");
		return new ARForm(new ARAdapter(ctx), "ARDBC:Query", fields);
	}

	public void loadConfig() throws ARException {
		List<AREntry> arEntries = formConfig.getEntries();
		for (AREntry arEntry : arEntries) {
			String description = arEntry.getFieldStringValue("Description");
			Map<String, String> fieldValues = new HashMap<>();
			for (String field : configFieldNames)
				fieldValues.put(field, arEntry.getFieldStringValue(field));
			config.put(description, fieldValues);
		}
	}

	public AREntry loadQuery(QualifierInfo qualInfo) throws ARException {
		return formQuery.getEntries(qualInfo).get(0);
	}

}
