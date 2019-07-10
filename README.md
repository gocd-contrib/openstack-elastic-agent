## GoCD OpenStack Elastic agent plugin


[![Join the chat at https://gitter.im/gocd/openstack-elastic-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/gocd/openstack-elastic-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


## Building the code base

To build the jar, run `./gradlew clean test assemble`

## Installation

Documentation for installation is available [here](INSTALL.md).

## TODO

3. Multi authentication support ( eg. each pipeline can have their own openstack credential )
4. More examples on how to create custom VM.
5. More unit tests

## License

```plain
Copyright 2019 ThoughtWorks, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
