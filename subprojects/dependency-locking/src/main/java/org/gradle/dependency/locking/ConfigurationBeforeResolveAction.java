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

package org.gradle.dependency.locking;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

class ConfigurationBeforeResolveAction implements Action<ResolvableDependencies> {

    private static final Logger LOGGER = Logging.getLogger(DependencyLockingPlugin.class);

    private final Path lockFilesRoot;
    private final DependencyConstraintHandler constraintsHandler;
    private final DependencyFactory dependencyFactory;

    public ConfigurationBeforeResolveAction(Path lockFilesRoot, DependencyConstraintHandler constraintsHandler, DependencyFactory dependencyFactory) {
        this.lockFilesRoot = lockFilesRoot;
        this.constraintsHandler = constraintsHandler;
        this.dependencyFactory = dependencyFactory;
    }

    @Override
    public void execute(ResolvableDependencies resolvableDependencies) {
        LOGGER.warn("Pre resolve hook for {}", resolvableDependencies.getName());
        Path lockFile = lockFilesRoot.resolve(resolvableDependencies.getName() + DependencyLockingPlugin.FILE_SUFFIX);
        if (Files.exists(lockFile)) {
            loadLockFile(resolvableDependencies.getName(), lockFile);
        }
    }

    private void loadLockFile(String configuration, Path lockFile) {
        try {
            for (String line : Files.readAllLines(lockFile, Charset.forName("UTF-8"))) {
                if (!line.isEmpty()) {
                    constraintsHandler.add(configuration, dependencyFactory.createDependencyConstraint(line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to transform lock file into constraints", e);
        }
    }

}
