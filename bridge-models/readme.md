# Bridge
Bridges are the mechanism the platform uses for connecting systems to the forms for working with and displaying the information. Bridges generally provide read only access into another system by containing the code for

* Accessing the system
* Querying the system
* Formatting the system response into a standard format

## Bridge Models
Bridge models are user defined data structures.  They are configured in the Platform Consoles.  First a Bridge plugin must be created.  Bridge model and plugin configuration is passed to the Bridge adapter.

The configurations that are pass to the Bridge adapter are:
| Data Field | Description |
|-----------|---------|
| Structure | structure define the shape of the data |
| Attribute | attributes make a name to the return data.  The mapping of the attribute associates the name with a specific field. Fields are the values you are looking to return from a bridge request. |
| Qualification | qualifications allow for a specific query to be requested.  Each Query can have a set of parameters that will be passed to the Bridge adapter for use in making the Bridge request.|

## Plugins
Plugins alow users to define the Bridge properties that will be passed to the Bridge adapter.  These include things like Base Urls, Usernames, Passwords, Access Keys and Secret Keys to name a few.