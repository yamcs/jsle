Java implementation for the CCSDS SLE (Space Link Extension) protocol.

Currently implemented services are:
 - Forward CLTU Service
 - Return All Frame Service
 
 Both user (client) and provider(server) are implemented.
 
 There is also a SLE to UDP bridge which implements a standalone SLE provider useful as frontend for a simulator.
 See below.
 
 TODO:
 - Return Channel Frame Service 
 
 
 This library is independent from Yamcs; it's only dependencies are 
 - [netty](https://netty.io/) used to implement the network communication
 - ASN.1 library [jasn1](https://www.beanit.com/asn1/) used to encode/decode the SLE messages.
 
 The [yamcs-sle](http://github.com/yamcs) package is based on this library and  offers data links implementation that allow Yamcs to connect to SLE.
 
 
## SLE to UDP bridge

The SLE to UDP bridge is useful to achieve a setup like this:

MCS with SLE (e.g. Yamcs) <--- RAF/FCLTU SLE --> SleUdpBridge <---- CLTU/AOS|TM frames/UDP ----> Simulator

There is one config file

Currently the bridge does not implement offline RAF service (contributions welcome :) ).


 