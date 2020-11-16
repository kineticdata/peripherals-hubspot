# Bridge
Bridges are the mechanism the platform uses for connecting systems to the forms for working with and displaying the information. Bridges generally provide read only access into another system by containing the code for

* Accessing the system
* Querying the system
* Formatting the system response into a standard format

## Bridge Adapters
Bridge adapters are the code component that Bridges leverage to make calls into source systems. Bridge adapters on configured for a space using Plugins.  

Once a Bridge has been configured a Bridge model defines the fields to retrieve and the and the parameters that are passed.  The Bridge adapter uses the Bridge model to inelegantly interact with the source system.  Then the Bridge adapter formats the data in a standard why for consumption by the Kinetic Platform. 

## Plugins
Plugins alow users to define the Bridge properties that will be passed to the Bridge adapter.  These include things like Base Urls, Usernames, Passwords, Access Keys and Secret Keys to name a few.