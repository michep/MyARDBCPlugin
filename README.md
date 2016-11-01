BMC Remedy ARDBC plugin for performing "SET queries", which use "IN" or "NOT IN" subqueries.

Example: you have a Request, to which one can be relate to a number of Items. And you need to build an UI to provide a way to relate new Items, which are not yet related, to Request. Requests and Items are related with "many-to-many" cardinality, so there is another entity Relationship which contains Request_ID and Items_ID foreign keys.

So what is the best way to select Items which are not yet related to Request?

The simpliest way in SQL would be query like this:

SELECT <Items_Fields> FROM Items WHERE Items.ID NOT IN (SELECT Relationship.Items_ID from Relationship WHERE Relationship.Request_ID = <ID_Of_Request>)

Bad news are that Remedy can't support forms with such a query to be created in Developer Studio, but can handle such queries with API calls.

To get use of this plugin you need to:
- register plugin in pluginsvr_config.xml:

<plugin>
  <name>PLUGIN_NAME</name>
  <type>ARDBC</type>
  <code>JAVA</code>
  <filename>PATH_TO_JAR/JAR_NAME.jar</filename>
  <classname>org.michep.ardbc.MyARDBCPlugin</classname>
  <userDefined>
    <debug>true</debug>
  </userDefined>
</plugin>

- register plugin in ar.conf/ar.cfg:

Server-Plugin-Alias: PLUGIN_NAME PLUGIN_NAME ARS_SERVER_NAME:JAVA_PLUGIN_PORT

- import definitions from ardbc_config.def to the Remedy server. There are two forms in it - ARDBC:Config and ARDBC:Query.

- create a record in ARDBC:Config form:
  Name = VENDOR_TABLE_NAME (e.g. "ITEMS_AVAILABLE_TO_RELATE_TO_REQUEST")
  Primary Form Name = Items
  Primary Form Relation Field ID = database ID of the field ID on Items form
  Subquery Form Name = Relationship
  Subquery Form Relation Field ID = database ID of the field Items_ID on Relationship form
  Subquery Operation = NOT IN

- in Developer Studio create new Vendor Form, select plugin PLUGIN_NAME, table VENDOR_TABLE_NAME and save it without adding any fields to it (only Request ID field will be added to form)

- copy field "Query GUID" from ARDBC:Query form and paste it to newly created vendor form (Database ID need to be the same - 400000777

- copy as many fields as you want from Items form to your vendor form, they will be result field (just like <Items_Fields> in the SQL query example above)

Now you are ready to perform a search on vendor form, to do it:
- create a record in ARDBC:Query form with a unique ID in Query ID field and qualification strings in Primary Form Qual and Subquery Form Qual fields, something like:
  Primary Form Qual => 'Item Status' = "Active"
  Subquery Form Qual => 'Relationship Status' = "Active" AND 'Request_ID' = "SOME_REQUEST_ID"

- open vendor form in Search mode, put in Query GUID field the value of the same field in newly created ARDBC:Query record and do search
