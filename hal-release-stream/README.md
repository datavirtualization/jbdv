# HAL
[HAL](http://en.wikipedia.org/wiki/HAL_9000) is the codename for the release coordination of the web management
console that can be used across products. An essential part of HAL is to provide a way to build the final master
console.

At the moment HAL includes the following extensions:

- PicketLink
- Teiid
- Switchyard

The core console and the extensions are compiled together into one final master console. This master console is
assembled as a zip file named `hal-console-<version>-overlay.zip`. The zip contains a layer which can be safley
unzipped into an existing WildFly / EAP installation.

# Profiles
The HAL build can be controlled by using one or several profiles:

- __&lt;no profile&gt;__: Whithout specifying a profile the console for the community (WildFly) version is built.
- __product__ *and* __eap__: Builds the console with EAP specific settings (module.xml, L&F).
- __dev__: Reduces the GWT compile time drastically. Restricts the languages to english and the browser permutations to firefox (cannot be combined with eap).

# Build
To build HAL use `mvn clean install` from the root directory and choose a combination of profiles from above.

# Install
Unzip `target/hal-console-<version>-overlay.zip` into an existing WildFly / EAP installation and restart the server.
