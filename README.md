## GoCD OpenStack Elastic agent plugin


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
2. Example "Plugin Settings"  ( This is the global settings and some properites can be overrided on the job level )
  * Go Server URL
     * eg. https://your-go-server:8154/go
  * Agent auto-register (in minutes)*
     * eg. 30
  * Openstack Endpoint
     * eg. https://your-openstack-public-endpoint:5000/v2.0
  * Openstack Tenant
     * eg. GoCD_Elastic_Agent_Playground
  * Openstack User
     * eg. gocdstack
  * Openstack Password
     * eg. gocdstack
  * Openstack VM Prefix ( this is the prefix added to the VM's hostname to distinguish elastic agent VM to others
     * eg. gocdea
  * Openstack Image ( this is the VM image ID from Openstack )
     * eg. d921abbb-772b-4c96-a150-798506f2a37b
  * Openstack Flavor ( this is the Flavor ID from Openstack )
     * eg fa8c735b-d477-4649-bb6a-8d58f2052971
  * Openstack Network ( this is the Network ID from Openstack )
    * eg 6d6ceece-6de5-4be5-8a5a-180151f91820
  * Openstack UserData
3. (Optional) Job level configuration.  Image ID,  Flavor ID,  Network ID and UserData can be overrided per job ).  Here is an example
```
          <job name="linux-job">
            <tasks>
              <exec command="echo">
                <arg>"Hello World"</arg>
              </exec>
            </tasks>
            <agentConfig pluginId="cd.go.contrib.elastic-agent.openstack">
              <property>
                <key>openstack_image_id</key>
                <value>d921abbb-772b-4c96-a150-798506f2a37b</value>
              </property>
              <property>
                <key>openstack_flavor_id</key>
                <value>3</value>
              </property>
              <property>
                <key>openstack_network_id</key>
                <value>5f3134bb-aced-4969-abc6-f16b7914a5f6</value>
              </property>
              <property>
                <key>openstack_userdata</key>
                <value>##!/bin/bash\necho "$(/sbin/ifconfig | /bin/grep -A 1 'eth0' | /usr/bin/tail -1 | /bin/cut -d ':' -f 2 | /bin/cut -d ' ' -f 1) $HOSTNAME.go.cd" $HOSTNAME &gt;&gt; /etc/hosts\nhostname "$HOSTNAME.go.cd"</value>
              </property>
            </agentConfig>
          </job>
```


## TODO

1. Openstack Keystone V3 API support
2. Multi tenant/project support
3. Multi authentication support ( eg. each pipeline can have their own openstack credential )
4. More examples on how to create custom VM.


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
