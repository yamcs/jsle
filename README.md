Java implementation for the CCSDS SLE (Space Link Extension) protocol.

Currently implemented are the user (i.e. client) part of the protocol for the:
 - Forward CLTU Service
 - Return All Frame Service
 
 TODO:
 - Return Channel Frame Service 
 - provider (i.e. server) part of the protocol
 
 
 This library is independent from Yamcs; it's only dependencies are 
 - [netty](https://netty.io/) used to implement the network communication
 - ASN.1 library [jasn1](https://www.beanit.com/asn1/) used to encode/decode the SLE messages.
 
 The [yamcs-sle](http://github.com/yamcs) package is based on this library and  offers data links implementation that allow Yamcs to connect to SLE.
 
 
