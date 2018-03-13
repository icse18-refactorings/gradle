/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.dependency.locking;

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.List;

class ConfigurationBeforeResolveAction implements Action<ResolvableDependencies> {

    private static final Logger LOGGER = Logging.getLogger(DependencyLockingPlugin.class);

    private final LockfileReader lockfileReader;
    private final DependencyConstraintHandler constraintsHandler;
    private final DependencyFactory dependencyFactory;
    private final DependencyLockingDataExchanger dataExchanger;

    public ConfigurationBeforeResolveAction(LockfileReader lockfileReader, DependencyConstraintHandler constraintsHandler, DependencyFactory dependencyFactory,
                                            DependencyLockingDataExchanger dataExchanger) {
        this.lockfileReader = lockfileReader;
        this.constraintsHandler = constraintsHandler;
        this.dependencyFactory = dependencyFactory;
        this.dataExchanger = dataExchanger;
    }

    @Override
    public void execute(ResolvableDependencies resolvableDependencies) {
        LOGGER.warn("Pre resolve hook for {}", resolvableDependencies.getName());
        if (dataExchanger.getLockFileHandling() != DependencyLockingDataExchanger.LockfileHandling.UPDATE_ALL) {
            addPreferConstraints(resolvableDependencies.getName(), lockfileReader.readLockFile(resolvableDependencies.getName()));
        }
        LOGGER.warn("Constraints: {}", resolvableDependencies.getDependencyConstraints());
    }

    private void addPreferConstraints(String configuration, List<String> lines) {
        for (String line : lines) {
            if (mustConstrainModule(line)) {
                DependencyConstraint dependencyConstraint = dependencyFactory.createDependencyConstraint(line);
                dependencyConstraint.because("dependency-locking in place");
                LOGGER.warn("Adding dependency constraint '{}:{}:{}' to configuration '{}'", dependencyConstraint.getGroup(), dependencyConstraint.getName(), dependencyConstraint.getVersion(), configuration);
                constraintsHandler.add(configuration, dependencyConstraint);
            }
        }
    }

    private boolean mustConstrainModule(String line) {
        for (String moduleToUpgrade : dataExchanger.getUpgradeModules()) {
            if (line.startsWith(moduleToUpgrade)) {
                return false;
            }
        }
        return true;
    }

}
