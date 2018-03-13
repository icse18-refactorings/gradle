/*
 * Copyright 2018 the original author or authors.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;     http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package org.gradle.dependency.locking;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DependencyLockTask extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(DependencyLockTask.class);

    private AtomicReference<DependencyLockingPlugin.LockfileHandling> lockfileHandling;
    private AtomicReference<List<String>> upgradeModules;

    @Option(option = "upgradeAllLocks", description = "Enables dependency locking upgrade mode, for all modules")
    public void setUpgradeAll(boolean upgradeAll) {
        LOGGER.warn("DependencyLockTask configured with {}", upgradeAll);
        if (upgradeAll) {
            lockfileHandling.compareAndSet(DependencyLockingPlugin.LockfileHandling.VALIDATE, DependencyLockingPlugin.LockfileHandling.UPDATE_ALL);
        }
    }

    @Option(option = "upgradeModuleLocks", description = "Enables dependency locking upgrade mode, the value of the option are module notations without version, comma separated.")
    public void setUpgradeModules(String modules) {
        LOGGER.warn("DependencyLockTask configured with {}", modules);
        this.upgradeModules.set(Arrays.asList(modules.split(",")));
    }

    @TaskAction
    public void triggerLockWriting() {
        LOGGER.warn("DependencyLockTask executing");
        if (lockfileHandling.compareAndSet(DependencyLockingPlugin.LockfileHandling.VALIDATE, DependencyLockingPlugin.LockfileHandling.CREATE)) {
            // TODO Need to write lockfile for all configurations that were already resolved
        }
    }

    public void setExchangeProperties(AtomicReference<DependencyLockingPlugin.LockfileHandling> lockfileHandling, AtomicReference<List<String>> upgradeModules) {
        this.lockfileHandling = lockfileHandling;
        this.upgradeModules = upgradeModules;
    }
}
