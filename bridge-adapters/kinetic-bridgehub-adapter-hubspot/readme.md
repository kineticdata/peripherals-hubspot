# HubSpot Bridge Adapter
An adapter for interacting with the HubSpot api

## Configuration Values
| Name         | Description                                                                                                                          |
|:-------------|:-------------------------------------------------------------------------------------------------------------------------------------|
| Access Token | A token created by [HubSpot private app](https://developers.hubspot.com/docs/api/private-apps) for integrating with the Hubspot API. |

## Example Configuration
| Name         | Value |
|:-------------| :--- |
| Access Token | 36....4-...d-...f-..0d-c64feerf8... |

## Supported Structures
| Name                    | Description |
| :---------------------- | :------------------------- |
| Companies               | Get a list of companies in the crm.  Uses v3 API.   |
| Contacts                | Get a list of contacts in the crm.  Uses v3 API.   |
| Tickets                 | Get a list of tickets in the crm.  Uses v3 API.   |
| Adhoc                   | Requires an accessor parameter.  |

## Configuration example
| Structure               | Qualification Mapping      | Description |
| :---------------------- | :------------------------- | :------------------------- |
| Companies               |                   | Returns a list of companies     |
| Companies               | associations=tickets | Returns a list of companies with related ticket ids (note to access id an attribute with `$.associations.tickets.results` must be configured) |
| Company                | id=${Company Id} | Returns only the company that matches the Id|
| Contacts                | id=14308069          | Retrieve a single Contact  |
| Contacts                   | body=${"query": "acme"} | Search contacts for matches to **acme** |
| Adhoc                   | /crm/v3/objects/companies?accessor=results | Returns a list of companies |
| Adhoc                   | /crm/v3/objects/tickets?accessor=projects&client_id=${Ticket Id} | Retrieve a ticket using Adhoc |

## Notes
* [JsonPath](https://github.com/json-path/JsonPath#path-examples) can be used to access nested values. The root of the path is the accessor for the Structure.
* This adapter has been tested with the 1.1.0 bridgehub adapter.
* The adapter only supports Access Token authentication at this time.  
    - To access the API key you must have Super Admin [permission](https://knowledge.hubspot.com/settings/hubspot-user-permissions-guide?__hstc=45788219.78c31a6a1ef939bde78914f7fa0fd849.1624607212906.1624607212906.1624607212906.1&__hssc=45788219.1.1624607212907&__hsfp=1000307879&_ga=2.230712409.756209440.1624607210-1620574245.1624607210#super-admin).
    - Visit [HubSpot](https://developers.hubspot.com/docs/api/private-apps) for instructions for creating a private app and getting an access token.
* Pagination and sort order are not supported by the adapter, but Harvest source api behavior is supported.  
* From more information about HubSpot api visit [HubSpot Developer Docs](https://developers.hubspot.com/docs/api/overview)
* This adapter requires an id parameter to be passed to retrieve an element.
* Using HubSpots [Search](https://developers.hubspot.com/docs/api/crm/search) functionality is supported.  Pass `body={Json object}` in the qualification mapping to instruct the adapter to leverage search.  Currently only Qualification that have a **Result Type** of __Multiple__ can leverage this functionality.  
    * Example Qualification Mapping for search using filterGroups:
```javascript
body = {
    filterGroups: [{
        filters: [{
            value: "Foo",
            propertyName: "name",
            operator: "EQ"
        }]
    }],
    properties: ["foo", "bar"]
}
```