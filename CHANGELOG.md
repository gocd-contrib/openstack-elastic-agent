### 0.11.0

 * Added cache for calls to OpenStack API
 * Make it possible to spin up more agents than needed at the moment.
 * Add randomness to agent's lifetime

### 0.10.0

 - Move to version 3.0 of the elastic agent endpoint, requires Go Server >= v18.2
 - Added tracking of agent's instance before registration in server to prevent too many instances being created

### 0.9.0

- Allow image fallback to previous ID when assigning work
- Allow empty image names

### 0.8.0

- Add Openstack Keystone V3 Authentication

### 0.7.0

 - Improve logic for when to create new instances in OpenStack.
   Will only match state Idle not Building, with a max limit based on profile, plugin or 10 as default in that order.
 - Added needed fields in forms.
 - Added tests and logging.

### 0.6.0

 - fix null reference when user data is not specified
 - make some properties in elastic profile optional and fallback to global plugin settings
 - allow flavor specified by name or ID
