### 0.7.0

 - Improve logic for when to create new instances in OpenStack.
   Will only match state Idle not Building, with a max limit based on profile, plugin or 10 as default in that order.
 - Added needed fields in forms.
 - Added tests and logging.
   
### 0.6.0

 - fix null reference when user data is not specified
 - make some properties in elastic profile optional and fallback to global plugin settings
 - allow flavor specified by name or ID
