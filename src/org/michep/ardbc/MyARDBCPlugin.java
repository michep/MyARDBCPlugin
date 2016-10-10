package org.michep.ardbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.michep.ars.ARAdapter;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.ArithmeticOrRelationalOperand;
import com.bmc.arsys.api.Constants;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.EntryListFieldInfo;
import com.bmc.arsys.api.OperandType;
import com.bmc.arsys.api.OutputInteger;
import com.bmc.arsys.api.QualifierInfo;
import com.bmc.arsys.api.QuerySourceForm;
import com.bmc.arsys.api.QuerySourceValues;
import com.bmc.arsys.api.RegularQuery;
import com.bmc.arsys.api.RelationalOperationInfo;
import com.bmc.arsys.api.SortInfo;
import com.bmc.arsys.api.Value;
import com.bmc.arsys.api.ValueSetQuery;
import com.bmc.arsys.api.VendorForm;
import com.bmc.arsys.pluginsvr.plugins.ARDBCPlugin;
import com.bmc.arsys.pluginsvr.plugins.ARPluginContext;
import com.bmc.arsys.pluginsvr.plugins.ARVendorField;

public class MyARDBCPlugin extends ARDBCPlugin {
	private Config config;
	private ARAdapter adapter;	

	@Override
	public void initialize(ARPluginContext ctx) throws ARException {
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "initialize()");
		config = new Config(ctx);
		adapter = new ARAdapter(ctx);
	}

	@Override
	public List<Entry> getListEntryWithFields(ARPluginContext ctx, String tableName, List<ARVendorField> fieldsList, long transId, QualifierInfo qualifier, List<SortInfo> sortList, List<EntryListFieldInfo> getListFields, int startAt, int maxRetrieve, OutputInteger numMatches) throws ARException{
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "getListEntryWithFields()");
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "formName = " + tableName);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "fieldList = " + fieldsList);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "transId = " + transId);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "qualifier = " + qualifier);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "sortList = " + sortList);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "getListFields = " + getListFields);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "startAt = " + startAt);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "maxRetrieve = " + maxRetrieve);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "numMatches = " + numMatches);

		config.loadConfig();

		Map<String, String> configOptions = config.getConfigOptions(tableName);

		QuerySourceForm subqueryForm = new QuerySourceForm(configOptions.get("Subquery Form Name"));
		ValueSetQuery subquery = new ValueSetQuery();		
		subquery.addFromSource(subqueryForm);
		subquery.addFromField(Integer.parseInt(configOptions.get("Subquery Form Relation FieldID")), subqueryForm);

		QualifierInfo subqueryQual = qualifier;

		adaptQualifier(subqueryQual, subqueryForm);

		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "subqueryQual v3 = " + subqueryQual);

		subquery.setQualifier(subqueryQual);

		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "subquery done");

		RegularQuery mainQuery = new RegularQuery();

		QuerySourceForm mainForm = new QuerySourceForm(configOptions.get("Primary Form Name"));
		mainQuery.addFromSource(mainForm);
		mainQuery.addFromField(Integer.parseInt(configOptions.get("Primary Form Result FieldID")), mainForm);

		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "mainquery done");

		ArithmeticOrRelationalOperand mainFieldOp = new ArithmeticOrRelationalOperand(Integer.parseInt(configOptions.get("Primary Form Relation FieldID")), mainForm);
		ArithmeticOrRelationalOperand subqueryOp = new ArithmeticOrRelationalOperand(subquery);
		int clause = configOptions.get("Subquery Operation").equals("0") ? Constants.AR_REL_OP_IN : Constants.AR_REL_OP_NOT_IN;
		RelationalOperationInfo relOp = new RelationalOperationInfo(clause, mainFieldOp, subqueryOp);
		QualifierInfo qual = new QualifierInfo(relOp);
		mainQuery.setQualifier(qual);

		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "mainquery qual done");

		adapter.impersonateUser(ctx.getUser());
		OutputInteger nMatch = new OutputInteger();
		List<QuerySourceValues> result = adapter.getListEntryObjects(mainQuery, 0, 0, false, nMatch);

		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "result = " + result);

		List<Entry> entryList = new ArrayList<Entry>();

		for (QuerySourceValues queryResVal : result) {
			ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "queryResVal = " + queryResVal);
			Map<Integer, Value> values = new HashMap<Integer, Value>();
			values = queryResVal.get(mainForm);
			ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "values = " + values);
			entryList.add(new Entry(values));
		}

		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "entryList = " + entryList);
		numMatches.setValue(nMatch.intValue());
		return entryList;
	}

	private void adaptQualifier(QualifierInfo inQual, QuerySourceForm subqueryForm) {
		int qualOperation = inQual.getOperation();
		if (qualOperation == Constants.AR_COND_OP_NOT)
			return;
		if ((qualOperation == Constants.AR_COND_OP_AND) || (qualOperation == Constants.AR_COND_OP_OR)) {
			adaptQualifier(inQual.getLeftOperand(), subqueryForm);
			adaptQualifier(inQual.getRightOperand(), subqueryForm);
		}
		if (qualOperation == Constants.AR_COND_OP_NOT)
			adaptQualifier(inQual.getNotOperand(), subqueryForm);
		if (qualOperation == Constants.AR_COND_OP_REL_OP) {
			if (inQual.getRelationalOperationInfo().getLeftOperand().getType() == OperandType.FIELDID) {
				int fieldId = ((Integer) inQual.getRelationalOperationInfo().getLeftOperand().getValue()).intValue();
				ArithmeticOrRelationalOperand op = new ArithmeticOrRelationalOperand(fieldId, subqueryForm);
				inQual.getRelationalOperationInfo().setLeftOperand(op);
			}
			if (inQual.getRelationalOperationInfo().getRightOperand().getType() == OperandType.FIELDID) {
				int fieldId = ((Integer) inQual.getRelationalOperationInfo().getRightOperand().getValue()).intValue();
				ArithmeticOrRelationalOperand op = new ArithmeticOrRelationalOperand(fieldId, subqueryForm);
				inQual.getRelationalOperationInfo().setRightOperand(op);
			}
		}
	}

	@Override
	public Entry getEntry(ARPluginContext ctx, String tableName, List<ARVendorField> fieldsList, long transId, String entryId, int[] idList) throws ARException {		
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "getEntry()");
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "formName = " + tableName);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "fieldsList = " + fieldsList);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "transId = " + transId);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "entryId = " + entryId);
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "idList = " + idList);
		Map<Integer, Value> values = new HashMap<Integer, Value>();
		values.put(1, new Value(entryId));
		values.put(8, new Value(""));
		Entry entry = new Entry(values);
		return entry;
	}

	@Override
	public void terminate(ARPluginContext ctx) throws ARException {
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "terminate()");
		adapter.terminate();
	}

	@Override
	public List<VendorForm> getListForms(ARPluginContext ctx) throws ARException {
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "getListForms()");
		List<VendorForm> vendorFormList = new ArrayList<VendorForm>();
		for (String configName : config.getConfigNames()) {
			vendorFormList.add(new VendorForm(ctx.getPluginInfo().getName(), configName));
			ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "config name = " + configName);
		}
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "plugin name = " + ctx.getPluginInfo().getName());
		return vendorFormList;
	}

	@Override
	public List<ARVendorField> getMultipleFields(ARPluginContext ctx, VendorForm vendorForm) throws ARException {
		ctx.logMessage(ARPluginContext.PLUGIN_LOG_LEVEL_INFO, "getMultipleFields()");
		List<ARVendorField> vendorFieldList = new ArrayList<ARVendorField>();
//		ARVendorField vendorField = new ARVendorField("result", 0, Constants.AR_DATA_TYPE_CHAR);
//		vendorFieldList.add(vendorField);
		return vendorFieldList;
	}
}
