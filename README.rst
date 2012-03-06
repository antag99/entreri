Entreri
=======

Entreri is an entity-component framework useful for avoiding the OO-hell that 
can develop when creating games in Java.

It is specially designed for high performance, extensively avoids branching, 
and features much better cache locality than using normal Java references. Using 
well-specified conventions, components are mapped into packed memory structures,
so that iteration is fast and cache friendly.

Java's garbage collection can move objects around, hurting locality when using 
Object arrays or collections. By using primitive arrays or buffers storing
managed data in blocks, all of your game data will be automatically stored in a 
cache friendly, and iteration friendly manner.

In addition, this requires less memory per instance because the actual
component data is stored in packed structures, avoiding the Java Object model
overhead normally associated with complex classes.

In off-the-cuff performance tests, garbage collection in other entity-component 
frameworks could cause performance to slow down by a factor of 2 to 4, while 
Entreri remained consistently fast (pre-GC performances were effectively 
identical).

Maven
~~~~~
 * version: 1.5.0
 * groupId: com.lhkbob.entreri
 * artifactId: entreri
