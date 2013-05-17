/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lhkbob.entreri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Utility function to validate a build log from the maven-invoker-plugin
 */
public final class InvokerUtils {
    private InvokerUtils() {
    }

    public static boolean validateLog(File baseDir) throws IOException {
        String baseName = baseDir.getCanonicalPath();
        String componentClass = baseName.substring(baseName.lastIndexOf('/') + 1);
        File logFile = new File(baseDir, "build.log");

        boolean compilationError = false;
        boolean expectedClassFailure = false;

        Pattern expectedPattern = Pattern
                .compile("\\[ERROR] .*" + componentClass + "\\.java:\\[.*] error: .*");

        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("[ERROR] COMPILATION ERROR :")) {
                compilationError = true;
            } else if (expectedPattern.matcher(line).matches()) {
                expectedClassFailure = true;
            }
        }
        reader.close();

        return compilationError && expectedClassFailure;
    }
}
