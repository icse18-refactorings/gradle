/*
 * Copyright 2018 the original author or authors.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;     http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package org.gradle.dependency.locking;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.concurrent.atomic.AtomicBoolean;

public class DependencyLockTask extends DefaultTask {

    private AtomicBoolean mustWrite;

    @TaskAction
    public void triggerLockWriting() {
        if (mustWrite.compareAndSet(false, true)) {
            // TODO Need to write all configurations that were resolved already.
        }
    }

    public void setMustWrite(AtomicBoolean mustWrite) {
        this.mustWrite = mustWrite;
    }
}
