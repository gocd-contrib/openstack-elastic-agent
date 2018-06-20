## GoCD OpenStack Elastic agent plugin


[![Join the chat at https://gitter.im/gocd/openstack-elastic-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/gocd/openstack-elastic-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


## Building the code base

To build the jar, run `./gradlew clean test assemble`

## Installation

1. After the jar is created,  create directory plugins/external under SERVER_WORK_DIR on go-server and copy the file to that directory then restart go-server
2. This elastic agent require custom image to work.  In general, you will want custom build VM for your build to speed up your job especially your job require a lot of external libraries and packages to run.
  1. At the minimum, have a VM with cloudinit installed ( most of the public Openstack Image has this pre-installed ).
  2. Download and save this file to your VM (https://raw.githubusercontent.com/gocd-contrib/openstack-elastic-agent-tools/master/cloudinit/cc_openstack_gocd.py).
    * For Linux with Python 2.6 installed ( eg RedHat/CentOS 6.0 ), save the python script to /usr/lib/python2.6/site-packages/cloudinit/config
    * For Linux with Python 2.7 installed ( eg RedHat/CentOS 7.0 ), save the python script to /usr/lib/python2.7/site-packages/cloudinit/config

**  It is possible to use Openstack UserData to achive the task of cc_openstack_gocd.py.  Since most people will need custom VM for their build especially when lot of packages are needed to pre-install, it is easy to use cc_openstack_gocd.py and leave the UserData for other purpose.

## Configuration

** This Openstack Elastic Agent plugin require custom image

1. Login to Go Server, choose "Plugins" under "Admin" tab.  Click the "wheel" icon next to "Openstack Elastic Agent Plugin", a "Plugin Settings" menu will pop up.
2. Example "Plugin Settings"  ( This is the global settings and some properties can be overriden on in the elastic profile )
  * Go Server URL
     * eg. https://your-go-server:8154/go
  * Agent Time To Live minimum (in minutes)*
     * eg. `30` if you want the agent to live for at least 30 minutes 
  * Agent Time To Live maximum (in minutes)*
     * eg. `60` if you want the agent to live a random amount between 30 and 60 minutes 
  * Openstack Endpoint
     * eg. https://your-openstack-public-endpoint:5000/v2.0
  * Openstack Tenant
     * eg. GoCD_Elastic_Agent_Playground
  * Openstack User
     * eg. gocdstack
  * Openstack Password
     * eg. gocdstack
  * Openstack VM Prefix (this is the prefix added to the VM's hostname to distinguish elastic agent VM to others)
     * eg. gocdea
  * Openstack Image (this is the VM image ID or image name from Openstack)
     * eg. `d921abbb-772b-4c96-a150-798506f2a37b`, `ubuntu-16.04`
  * Openstack Image Name -> Image ID Cache TTL ( the cache keeps the image ID of the image name  for the given amount of minutes)
     * eg. `30`
  * Allow Use of Previous Openstack Image 
     * if the previous image ID for given image name should be used as fallback
  * Openstack Flavor (this is the Flavor ID or flavor name from Openstack)
     * eg `fa8c735b-d477-4649-bb6a-8d58f2052971`, `12234`, `m1.small`
  * Openstack Network (this is the Network ID from Openstack)
    * eg 6d6ceece-6de5-4be5-8a5a-180151f91820
  * Default Min Instance Limit (only relevant when there is a need for agents of that profile)
    * eg 2
  * Default Max Instance Limit (for each profile)
    * eg 5
  * Openstack UserData
3. Elastic profile configuration.  Image ID,  Flavor ID,  Network ID and UserData can be overriden ).  Here is an example which sets a different flavor
```xml
<profile id="2" pluginId="cd.go.contrib.elastic-agent.openstack">
  <property>
    <key>openstack_image_id</key>
  </property>
  <property>
    <key>openstack_flavor_id</key>
    <value>v.c1.m3076.d5.e10</value>
  </property>
  <property>
    <key>openstack_network_id</key>
  </property>
  <property>
    <key>openstack_security_group</key>
    <value>default</value>
  </property>
  <property>
    <key>openstack_keypair</key>
    <value>go</value>
  </property>
  <property>
    <key>openstack_max_instance_limit</key>
    <value>5</value>
  </property>
  <property>
    <key>openstack_userdata</key>
  </property>
</profile>
```


## TODO

2. Multi tenant/project support
3. Multi authentication support ( eg. each pipeline can have their own openstack credential )
4. More examples on how to create custom VM.
5. More unit tests

## License

```plain
Copyright 2016 ThoughtWorks, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
