package org.michep.ars;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Field;

public class ARForm {
	private String formName;
	private Map<Integer, String> fields;
	private ARAdapter adapter;
	private List<Field> fieldList;

	public ARForm(ARAdapter adapter, String formName, Map<Integer, String> fields) throws ARException {
		this.adapter = adapter;
		this.formName = formName;
		this.fields = fields;
		fieldList = this.adapter.getFields(formName, fields);
	}

	public List<AREntry> getEntries() throws ARException {
		List<Entry> entries = adapter.getEntries(formName, "", fields);
		List<AREntry> arentries = new ArrayList<AREntry>();
		for (Entry entry : entries) {
			arentries.add(new AREntry(entry, fields, fieldList));
		}
		return arentries;
	}

	public String createEntry(AREntry arentry) throws ARException {
		return adapter.createEntry(formName, arentry);
	}

	public List<Field> getFieldList() throws ARException {
		return adapter.getFields(formName, fields);
	}

	public AREntry getNewEntry() {
		return new AREntry(fields, fieldList);
	}
}
