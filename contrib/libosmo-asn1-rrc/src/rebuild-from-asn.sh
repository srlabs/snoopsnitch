#!/bin/bash
ASN=../asn
asn1c -fcompound-names -fnative-types -gen-PER $ASN/Class-definitions.asn $ASN/Constant-definitions.asn $ASN/InformationElements.asn $ASN/Internode-definitions.asn $ASN/PDU-definitions.asn
find . -type l -exec rm \{\} \;
mv *.h ../include/
