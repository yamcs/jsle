Java implementation for the CCSDS SLE (Space Link Extension) protocol.

Currently implemented services are:
 - Forward CLTU Service
 - Return All Frame Service
 - Return Channel Frame Service
 
 Both user (client) and provider(server) are implemented.
 
 There is also a SLE to UDP bridge which implements a standalone SLE provider useful as frontend for a simulator.
 See below.

 
 This library is independent from Yamcs; it's only dependencies are 
 - [netty](https://netty.io/) used to implement the network communication
 - ASN.1 library [jasn1](https://www.beanit.com/asn1/) used to encode/decode the SLE messages.
 
 The [yamcs-sle](http://github.com/yamcs) package is based on this library and  offers data links implementation that allow Yamcs to connect to SLE.

TODO:
 add some automated tests
 
 
## SLE to UDP bridge

The SLE to UDP bridge is useful for testing a SLE connection to an existing simulator in a setup like this:

MCS with SLE (e.g. Yamcs) <--- RAF/RCF/FCLTU SLE --> SleUdpBridge <---- CLTU/AOS|TM frames/UDP ----> Simulator

The configuration is in bridge.properties. logging.properties can be adjusted to increase the logging at SLE level.

To start the bridge, please launch the script:
./udp-sle-bridge.sh

To start the bridge directly in the source repository:
mvn exec:java


### Known Problems and Limitations of the SLE to UDP bridge
* RCF works effectively as RAF, no filtering on VCID is performed by the provider




 
