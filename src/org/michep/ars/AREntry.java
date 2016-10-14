package org.michep.ars;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmc.arsys.api.Constants;
import com.bmc.arsys.api.DataType;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.EnumItem;
import com.bmc.arsys.api.Field;
import com.bmc.arsys.api.SelectionFieldLimit;
import com.bmc.arsys.api.Value;

public class AREntry {

	private Map<String, Value> mapByFieldName = new HashMap<>();
	private Map<Integer, Value> mapByFieldID = new HashMap<>();
	private Map<Integer, String> fields;
	private Entry entry;
	private List<Field> fieldList;

	public AREntry(Map<Integer, String> fields, List<Field> fieldList) {
		this(new Entry(), fields, fieldList);
	}

	public AREntry(Entry entry, Map<Integer, String> fields, List<Field> fieldList) {
		this.entry = entry;
		this.fieldList = fieldList;
		this.fields = fields;
		for (int fid : fields.keySet()) {
			mapByFieldID.put(fid, entry.get(fid));
			mapByFieldName.put(fields.get(fid), entry.get(fid));
		}
	}

	public String getFieldStringValue(int fieldID) {
		return mapByFieldID.get(fieldID).toString();
	}

	public String getFieldStringValue(String fieldName) {
		return mapByFieldName.get(fieldName).toString();
	}

	// public Value getFieldValue(int fieldID) {
	// return mapByFieldID.get(fieldID);
	// }
	//
	// public Value getFieldValue(String fieldName) {
	// return mapByFieldName.get(fieldName);
	// }

	public void setFieldValue(int fieldID, String value) {
		Field fieldObj = fieldList.get(getFieldObjIndex(fieldID));
		int dataType = fieldObj.getDataType();
		Value v = new Value("");
		if (dataType == Constants.AR_DATA_TYPE_ENUM) {
			SelectionFieldLimit enumLimit = (SelectionFieldLimit) fieldObj.getFieldLimit();
			List<EnumItem> enumList = enumLimit.getValues();
			for (EnumItem item : enumList) {
				if (item.getEnumItemName().equals(value)) {
					v = new Value(item.getEnumItemNumber(), DataType.ENUM);
					break;
				}
			}
		} else if (dataType == Constants.AR_DATA_TYPE_INTEGER || dataType == Constants.AR_DATA_TYPE_TIME)
			v = new Value(Integer.parseInt(value), DataType.INTEGER);
		else if (dataType == Constants.AR_DATA_TYPE_CHAR)
			v = new Value(value, DataType.CHAR);
		mapByFieldID.put(fieldID, v);
		mapByFieldName.put(fields.get(fieldID), v);
	}

	public void setFieldValue(String fieldName, String value) {
		setFieldValue(getFieldID(fieldName), value);
	}

	public void setFieldValues(Map<String, String> values) {
		for (String fieldName : values.keySet())
			setFieldValue(fieldName, values.get(fieldName));
	}

	public Entry buildEntry() {
		entry = new Entry(mapByFieldID);
		return entry;

	}

	public Entry getEntry() {
		return entry;
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	private int getFieldID(String fieldName) {
		for (int id : fields.keySet())
			if (fields.get(id).equals(fieldName))
				return id;
		return -1;
	}

	private int getFieldObjIndex(int fieldID) {
		int i;
		for (i = 0; i < fieldList.size(); i++)
			if (fieldList.get(i).getFieldID() == fieldID)
				break;
		return i;
	}
}
