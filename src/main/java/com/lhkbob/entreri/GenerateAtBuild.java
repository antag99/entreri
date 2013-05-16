package com.lhkbob.entreri;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GenerateAtBuild {
    // FIXME I can get the processor to handle all components by filtering over all
    // received types. Is this better? It does slow the build down and makes testing both
    // the janino and compiled classes trickier, on the other hand always processing them
    // means that a) I could remove the janino dependency (not sure if i want to though),
    // b) all components are build-time checked
    //
    // But, what if intellij doesn't integrate well if APT?
}
