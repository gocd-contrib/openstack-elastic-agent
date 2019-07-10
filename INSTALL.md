# GoCD Elastic agent plugin for OpenStack

Table of Contents
=================

  * [Requirements](#requirements)
  * [Installation](#installation)
  * [Configuration](#configuration)
    - [Configure a Cluster Profile](#configure-a-cluster-profile)
    - [Create an Elastic Profile](#create-an-elastic-profile)
    - [Configure a Job to use an Elastic Agent Profile](#configure-a-job-to-use-an-elastic-agent-profile)

## Requirements

* GoCD server version `v19.5.0` or above
* OpenStack Cluster

## Installation

* Copy the file `build/libs/openstack-elastic-agents-VERSION.jar` to the GoCD server under `${GO_SERVER_DIR}/plugins/external`
and restart the server.
* The `GO_SERVER_DIR` is usually `/var/lib/go-server` on **Linux** and `C:\Program Files\Go Server` on **Windows**.
* This elastic agent require custom image to work.  In general, you will want custom build VM for your build to speed up your job especially your job require a lot of external libraries and packages to run.
  1. At the minimum, have a VM with cloudinit installed ( most of the public OpenStack Image has this pre-installed ).
  2. Download and save this file to your VM (https://raw.githubusercontent.com/gocd-contrib/openstack-elastic-agent-tools/master/cloudinit/cc_openstack_gocd.py).
    * For Linux with Python 2.6 installed ( eg RedHat/CentOS 6.0 ), save the python script to /usr/lib/python2.6/site-packages/cloudinit/config
    * For Linux with Python 2.7 installed ( eg RedHat/CentOS 7.0 ), save the python script to /usr/lib/python2.7/site-packages/cloudinit/config

**  It is possible to use OpenStack UserData to achive the task of cc_openstack_gocd.py.  Since most people will need custom VM for their build especially when lot of packages are needed to pre-install, it is easy to use cc_openstack_gocd.py and leave the UserData for other purpose.


## Configuration

** This OpenStack Elastic Agent plugin require custom image

### Configure a Cluster Profile

The cluster profile settings are used to provide cluster level configurations for the plugin. Configurations such as docker server configuration and private registry settings are provided in cluster profile settings.

1. Login to `GoCD server` as admin and navigate to **_Admin_** _>_ **_Elastic Profile_**

    ![Elastic Profiles][1]

2. Click on **_Add Cluster Profile_**. Select `OpenStack Elastic Agent Plugin` from the plugin ID dropdown. 

    ![Cluster Profile basic settings][2]

| Field Name                          |Mandatory| Description                             |
|-------------------------------------|---------|-----------------------------------------|
| **Go Server URL**                   | Yes     | GoCD server url(`https://YOUR_HOST_OR_IP_ADDRESS:8154/go`). The elastic agent will use this URL to register itself with GoCD. |
| **Agent Pending Register Timeout**  | Yes     | Agent auto-register timeout(in minutes). Plugin will kill the agent instance if it fails to register within provided time limits |
| **Agent Time To Live minimum**      | Yes     | e.g. `30` if you want the agent to live for at least 30 minutes |
| **Agent Time To Live maximum**      | Yes     | A random TTL between minimum and maximum will be generated, e.g. min 30 and max 60 results in between 30 and 60 minutes |
| **OpenStack Endpoint URL**          | Yes     | OpenStack cluster URL e.g. https://your-openstack-public-endpoint:5000/v3.0 |
| **OpenStack Keystone Version**      | Yes     | |
| **OpenStack Domain**                | Yes     | |
| **OpenStack Tenant**                | Yes     | |
| **OpenStack User**                  | Yes     | |
| **OpenStack Password**              | Yes     | |
| **OpenStack VM Prefix**             | Yes     | the prefix added to the VM's hostname to distinguish elastic agent VM to others |
| **OpenStack Image**                 | Yes     | the default VM image ID or image name |
| **OpenStack Image Name -> Image ID Cache TTL** | Yes        | in minutes e.g. `30` |
| **Allow Use of Previous OpenStack Image ID**   | Yes        | when Image Name has been updated with new Image ID |
| **OpenStack Flavor**                | Yes     | the default Flavor ID or flavor name |
| **OpenStack Network**               | Yes     | the default Flavor ID or flavor name |
| **Default minimum instance limit**  | Yes     | only relevant when there is a need for agents of that profile |
| **Default maximum instance limit**  | Yes     | For each profile |
| **OpenStack UserData**              | No      | |
| **Delete instances in ERROR state** | Yes     | |
| **Disable SSL verification**        | Yes     | when self-signed certificates are used |


### Create an elastic profile

    The Elastic Agent Profile is used to define the configuration of a OpenStack instance (GoCD agent). The profile is used to configure the image, flavor, etc...

1. Login to `GoCD server` as admin and navigate to **_Admin_** _>_ **_Elastic Profiles_**

    ![Elastic Profiles][3]

2. Click on **_New Elastic Agent Profile_** to create new elastic agent profile for a cluster.

    ![Create elastic profile][4]

    | Field Name                       | Mandatory | Description            |
    |----------------------------------|-----------|------------------------|
    | **Id**                           | Yes       | Unique id for current profile  |
    | **Cluster Profile ID**           | Yes       | Select cluster for `OpenStack Elastic Agent Plugin`  |
    | **OpenStack Image Name/ID**      | No        | GoCD elastic agent OpenStack image. |
    | **OpenStack Flavor Name/ID**     | No        | OpenStack flavor. |
    | **OpenStack Network ID**         | No        | |
    | **OpenStack Security Group**     | Yes       | |
    | **OpenStack Keypair**            | Yes       | |
    | **Minimum Instance Limit**       | No        | only relevant when there is a need for agents of this profile |
    | **Max Instance Limit**           | No        | |
    | **Max Completed Jobs per Agent** | No        | |
    | **OpenStack UserData**           | No        | |


### Configure a job to use an elastic agent profile

1. Click the gear icon on **_Pipeline_**

    ![Pipeline][5]

2. Create/Edit a job
3. Enter the `unique id` of an elastic profile in Job Settings

    ![Configure a job][7]

4. Save your changes

[1]: images/elastic_profiles_spa.png     "Elastic Profiles"
[2]: images/cluster-profiles/basic-settings.png    "Cluster Profile basic settings"
[3]: images/profiles_page.png  "Elastic profiles"
[4]: images/profile.png "Create elastic profile"
[5]: images/pipeline.png  "Pipeline"
[7]: images/configure-job.png  "Configure a job"
