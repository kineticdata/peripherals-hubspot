{
  'info' => {
    'api_key' => "",
    'api_location' => "https://api.hubapi.com",
    'enable_debug_logging' => 'true'
  },
  'parameters' => {
    'error_handling' => 'Error Message',
    'method' => 'POST',
    'path' => '/crm/v3/objects/tickets',
    'body' => '{"properties":{"hs_pipeline":0,"hs_ticket_category":"RFE","hs_pipeline_stage":1,"hs_ticket_priority":"HIGH","subject":"test ticket from api"}}'
  }
}
