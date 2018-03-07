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
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.dependency.locking.exception.LockOutOfDateException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class ConfigurationAfterResolveAction implements Action<ResolvableDependencies> {

    private static final Logger LOGGER = Logging.getLogger(DependencyLockingPlugin.class);
    private Path lockFilesRoot;

    public ConfigurationAfterResolveAction(Path lockFilesRoot) {
        this.lockFilesRoot = lockFilesRoot;
    }

    @Override
    public void execute(ResolvableDependencies resolvableDependencies) {
        Path lockFile = lockFilesRoot.resolve(resolvableDependencies.getName() + DependencyLockingPlugin.FILE_SUFFIX);
        if (Files.exists(lockFile)) {
            validateLockAligned(resolvableDependencies, lockFile);
        }
    }

    private void validateLockAligned(ResolvableDependencies resolvableDependencies, Path lockFile) {
        Map<String, String> modules = new TreeMap<String, String>();
        LOGGER.warn("Post resolve hook for {}", resolvableDependencies.getName());
        for (ResolvedComponentResult resolvedComponentResult : resolvableDependencies.getResolutionResult().getAllComponents()) {
            if (resolvedComponentResult.getId() instanceof ModuleComponentIdentifier) {
                ModuleComponentIdentifier id = (ModuleComponentIdentifier) resolvedComponentResult.getId();
                modules.put(id.getGroup() + ":" + id.getModule(), id.getVersion());
            }
        }
        LOGGER.warn("Found the following modules:\n\t{}", modules);
        List<String> lines = null;
        try {
            lines = Files.readAllLines(lockFile, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load lock file");
        }
        for (String line : lines) {
            if (!line.isEmpty()) {
                String module = line.substring(0, line.lastIndexOf(':'));
                String version = modules.get(module);
                if (version == null) {
                    throw new LockOutOfDateException("Lock file contained module '" + line + "' but it is not part of the resolved modules");
                } else if (!line.contains(version)) {
                    throw new LockOutOfDateException("Lock file expected '" + line + "' but resolution result was '" + module + ":" + version + "'");
                }
            }
        }
    }
}
