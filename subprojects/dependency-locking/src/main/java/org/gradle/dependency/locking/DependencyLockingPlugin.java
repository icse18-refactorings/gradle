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
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Incubating
public class DependencyLockingPlugin implements Plugin<ProjectInternal> {

    private static final Logger LOGGER = Logging.getLogger(DependencyLockingPlugin.class);

    enum LockfileHandling {
        VALIDATE,
        CREATE,
        UPDATE_ALL
    }

    private final AtomicReference<LockfileHandling> lockfileHandling = new AtomicReference<LockfileHandling>(LockfileHandling.VALIDATE);
    private final AtomicReference<List<String>> upgradeModules = new AtomicReference<List<String>>(Collections.<String>emptyList());

    private LockFileReaderWriter lockFileReaderWriter;
    private final DependencyFactory dependencyFactory;

    @Inject
    public DependencyLockingPlugin(DependencyFactory dependencyFactory) {
        this.dependencyFactory = dependencyFactory;
    }

    @Override
    public void apply(final ProjectInternal project) {
        LOGGER.warn("Applying dependency-locking plugin");
        lockFileReaderWriter = new LockFileReaderWriter(project);

        DependencyLockTask saveDependencyLocks = project.getTasks().create("saveDependencyLocks", DependencyLockTask.class);
        saveDependencyLocks.setExchangeProperties(lockfileHandling, upgradeModules);

        project.getConfigurations().all(new Action<Configuration>() {
            @Override
            public void execute(Configuration configuration) {
                if (configuration.isCanBeResolved()) {
                    LOGGER.warn("Adding hook to configuration {}", configuration.getName());
                    configuration.getIncoming().beforeResolve(new ConfigurationBeforeResolveAction(lockFileReaderWriter, project.getDependencies().getConstraints(), dependencyFactory, lockfileHandling, upgradeModules));
                    configuration.getIncoming().afterResolve(new ConfigurationAfterResolveAction(lockFileReaderWriter, lockfileHandling, upgradeModules));
                }
            }
        });
    }
}
