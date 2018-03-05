/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.gradle.dependency.locking

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class DependencyLockingPluginIntegrationTest extends AbstractIntegrationSpec {

    def 'basic test'() {
        buildFile << """
apply plugin: 'java'
apply plugin: 'dependency-locking'
"""
        expect:
        succeeds 'dependencies'
        file('dependency-locks').exists()
    }

    def 'test with lock file'() {
        buildFile << """
apply plugin: 'dependency-locking'

configurations {
    lockedConf
}

dependencies {
    lockedConf 'junit:junit:4.12'
}
"""
        file('dependency-locks', 'lockedConf.lockfile') << """
junit:junit:4.12
"""
        expect:
        succeeds 'dependencies'
    }
}
