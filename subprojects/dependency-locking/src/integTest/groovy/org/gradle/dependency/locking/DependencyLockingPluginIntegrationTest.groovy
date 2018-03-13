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

import org.gradle.integtests.fixtures.AbstractDependencyResolutionTest

import static org.gradle.util.Matchers.containsText

class DependencyLockingPluginIntegrationTest extends AbstractDependencyResolutionTest {

    def 'succeeds with lock file present'() {
        mavenRepo.module('org', 'foo', '1.0').publish()

        buildFile << """
apply plugin: 'dependency-locking'

repositories {
    maven {
        name 'repo'
        url '${mavenRepo.uri}'
    }
}
configurations {
    lockedConf
}

dependencies {
    lockedConf 'org:foo:1.+'
}
"""

        file('gradle', 'dependency-locks', 'lockedConf.lockfile') << """
org:foo:1.0
"""
        expect:
        succeeds 'dependencies'
    }

    def 'fails with out-of-date lock file'() {
        mavenRepo.module('org', 'foo', '1.0').publish()
        mavenRepo.module('org', 'foo', '1.1').publish()

        buildFile << """
apply plugin: 'dependency-locking'

repositories {
    maven {
        name 'repo'
        url '${mavenRepo.uri}'
    }
}
configurations {
    lockedConf
}

dependencies {
    constraints {
        lockedConf('org:foo') {
            version {
                prefer '1.1'
            }
        }
    }
    lockedConf 'org:foo:1.+'
}
"""

        file('gradle', 'dependency-locks', 'lockedConf.lockfile') << """
org:foo:1.0
"""
        when:
        fails 'dependencies'

        then:
//        failure.assertThatCause(containsText(LockOutOfDateException.class.name)) // Fails to find the exception name in the cause
        failure.assertThatCause(containsText("Lock file expected 'org:foo:1.0' but resolution result was 'org:foo:1.1'"))
    }

    def 'fails when lock file entry not resolved'() {
        mavenRepo.module('org', 'foo', '1.0').publish()
        mavenRepo.module('org', 'bar', '1.0').publish()

        buildFile << """
apply plugin: 'dependency-locking'

repositories {
    maven {
        name 'repo'
        url '${mavenRepo.uri}'
    }
}
configurations {
    lockedConf
}

dependencies {
    lockedConf 'org:foo:1.+'
}
"""

        file('gradle', 'dependency-locks', 'lockedConf.lockfile') << """
org:bar:1.0
org:foo:1.0
"""
        when:
        fails 'dependencies'

        then:
        failure.assertThatCause(containsText("Lock file contained module 'org:bar:1.0' but it is not part of the resolved modules"))
    }

    def 'writes dependency lock file when requested'() {
        mavenRepo.module('org', 'foo', '1.0').publish()
        mavenRepo.module('org', 'bar', '1.0').publish()

        buildFile << """
apply plugin: 'dependency-locking'

repositories {
    maven {
        name 'repo'
        url '${mavenRepo.uri}'
    }
}
configurations {
    lockedConf
}

dependencies {
    lockedConf 'org:foo:1.+'
    lockedConf 'org:bar:1.+'
}
"""

        when:
        succeeds'dependencies', 'saveDependencyLocks'

        then:
        def lockFile = file('gradle', 'dependency-locks', 'lockedConf.lockfile')
        lockFile.exists()
        lockFile.text == """# This is a Gradle generated file for dependency locking.
# Manual edits can break the build and are not advised.
# This file is expected to be part of source control.
org:bar:1.0
org:foo:1.0
"""

    }

    def 'allows --upgradeAllLocks option to task'() {
        buildFile << """
apply plugin: 'dependency-locking'
"""
        expect:
        succeeds 'saveDependencyLocks', '--upgradeAllLocks'
    }

    def 'allows --upgradeModuleLocks option to task'() {
        buildFile << """
apply plugin: 'dependency-locking'
"""
        expect:
        succeeds 'saveDependencyLocks', '--upgradeModuleLocks', 'org:foo'
    }

    def 'upgrades lock file'() {
        mavenRepo.module('org', 'foo', '1.0').publish()
        mavenRepo.module('org', 'foo', '1.1').publish()

        buildFile << """
apply plugin: 'dependency-locking'

repositories {
    maven {
        name 'repo'
        url '${mavenRepo.uri}'
    }
}
configurations {
    lockedConf
}

dependencies {
    constraints {
        lockedConf('org:foo') {
            version {
                prefer '1.1'
            }
        }
    }
    lockedConf 'org:foo:1.+'
}
"""

        file('gradle', 'dependency-locks', 'lockedConf.lockfile') << """
org:foo:1.0
"""
        when:
        succeeds 'dependencies', 'saveDependencyLocks', '--upgradeAllLocks'

        then:
        def lockFile = file('gradle', 'dependency-locks', 'lockedConf.lockfile')
        lockFile.text == """# This is a Gradle generated file for dependency locking.
# Manual edits can break the build and are not advised.
# This file is expected to be part of source control.
org:foo:1.1
"""
    }

    def 'partially upgrades lock file'() {
        mavenRepo.module('org', 'foo', '1.0').publish()
        mavenRepo.module('org', 'foo', '1.1').publish()
        mavenRepo.module('org', 'bar', '1.0').publish()
        mavenRepo.module('org', 'bar', '1.1').publish()

        buildFile << """
apply plugin: 'dependency-locking'

repositories {
    maven {
        name 'repo'
        url '${mavenRepo.uri}'
    }
}
configurations {
    lockedConf
}

dependencies {
    constraints {
        lockedConf('org:foo') {
            version {
                prefer '1.1'
            }
        }
    }
    lockedConf 'org:foo:[1.0, 1.1]'
    lockedConf 'org:bar:[1.0, 1.1]'
}
"""

        file('gradle', 'dependency-locks', 'lockedConf.lockfile') << """
org:bar:1.0
org:foo:1.0
"""
        when:
        succeeds 'dependencies', 'saveDependencyLocks', '--upgradeModuleLocks', 'org:foo'

        then:
        def lockFile = file('gradle', 'dependency-locks', 'lockedConf.lockfile')
        lockFile.text == """# This is a Gradle generated file for dependency locking.
# Manual edits can break the build and are not advised.
# This file is expected to be part of source control.
org:bar:1.0
org:foo:1.1
"""
    }

}
