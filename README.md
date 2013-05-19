# Entreri

Entreri is a data-oriented entity-component framework designed for high 
performance applications and games. It scales well to tens of thousands of 
instances, in both memory and performance, and fits data models that require 
many similar objects of the same type, objects needing runtime aspects
, and combinations thereof.

Using annotation processing over component interfaces, the underlying
is packed into primitive arrays to improve cache locality. Fly-weight proxy instances
are then used to access the packed data. Other advanced features include runtime
decoration of components with new data, and a multi-threading oriented task
API to process the entity system.

Java's garbage collection can move objects around, hurting locality when using 
Object arrays or collections. By using primitive arrays or buffers storing
managed data in blocks, all of your game data will be automatically stored in a
cache friendly, and iteration friendly manner.

In off-the-cuff performance tests, garbage collection in other entity-component
frameworks could cause performance to slow down by a factor of 2 to 4, while 
Entreri remained consistently fast (pre-GC performances near identical).

## Maven

Entreri can be easily added to a [Maven][] project with the XML snippet below.
The Eclipse plugin, [m2e][], can be used for Maven/Eclipse integration.

    <dependency>
      <groupId>com.lhkbob.entreri</groupId>
      <artifactId>entreri</artifactId>
      <version>1.7.0</version>
    </dependency>
    
[Maven]: http://maven.apache.org
[m2e]: http://eclipse.org/m2e

## Release Notes

### 1.7.0
* Drastically simplify `Component` definition by using interfaces and APT.
* Remove `IndexedDataStore` to consolidate property package.
* Make `Component`, `Entity` and `EntitySystem` interfaces and hide their implementations
  in an impl package.
* Remove `ComponentData`. Types now extend `Component` directly as sub-interfaces. The
  onSet() method has also been removed, but the same functionality can be used with the
  `@SharedInstance` annotation.
* Component proxy implementations generated at build time using an annotation
  processor.

### 1.6.1
* Remove `Entity.getIfModified()` method because its semantics were vague and unhelpful.
* Make version numbers unique within a component type.
* Simplify `isEnabled()` and `getVersion()` logic for invalid components.

### 1.6.0
* Completely replace old Controller API with a multi-threading oriented Task 
  and Job API.
* Add versioning to components so that properly implemented types report when 
  their data is changed.
* Entities and components can now be owned by each other (and other types 
  implementing Owner), allowing components to be grouped together and have the
  same lifetime automatically.
* Add `Required` annotation to support component type dependencies. When a 
  component is added to an entity, all required components are also added, and
  are owned by the initiating component.
* Improve toString() methods for Component and Entity.
* Add `SharedInstance` documentation annotation for when unmanaged instances
  are reused by a ComponentData instance.

### 1.5.3
* Improve performance in default Property implementations by restricting them
  to a single primitive per component (removing a multiply and add on access).
* Expose multi-element supporting IndexedDataStores that were previously hidden
  inside the provided Property implementations.
* Simplify Phase enum and usage to not need ALL value.
* Fix bug with time delta calculation.
* Improve Result API in ControllerManage to not cause a type explosion.
* Add @Clone attribute to allow more flexibility in `PropertyFactory.clone()`
  without requiring you to implement an entire PropertyFactory.

### 1.5.2
* Improve PropertyFactory API to better support custom Property definitions 
  that want to define new annotations to control default behavior, etc.
* Replace Controller data storage using Key objects with a simple Result API
  that allows Controllers to express interest in data and have that injected
  using custom interfaces defined by the computing controller.

### 1.5.1
* Add protected onSet(int) method to ComponentData to better support 
  ComponentData's that rely on unmanaged fields for caching.
* Add functionality to estimate memory usage by component type.
* Update ControllerManage to record run time performance of each controller,
  for the last executed frame.
* Changed the no-argument process() method in ControllerManager to use the
  real change in time from the last frame, instead of a fixed time delta.
   