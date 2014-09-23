/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#ifndef	_SupportedCarrierCombination_ext_H_
#define	_SupportedCarrierCombination_ext_H_


#include <asn_application.h>

/* Including external dependencies */
#include <BOOLEAN.h>
#include <constr_SEQUENCE.h>

#ifdef __cplusplus
extern "C" {
#endif

/* SupportedCarrierCombination-ext */
typedef struct SupportedCarrierCombination_ext {
	BOOLEAN_t	 carrierCombination14;
	BOOLEAN_t	 carrierCombination41;
	BOOLEAN_t	 carrierCombination15;
	BOOLEAN_t	 carrierCombination51;
	BOOLEAN_t	 carrierCombination16;
	BOOLEAN_t	 carrierCombination61;
	BOOLEAN_t	 carrierCombination17;
	BOOLEAN_t	 carrierCombination71;
	BOOLEAN_t	 carrierCombination23;
	BOOLEAN_t	 carrierCombination32;
	BOOLEAN_t	 carrierCombination24;
	BOOLEAN_t	 carrierCombination42;
	BOOLEAN_t	 carrierCombination25;
	BOOLEAN_t	 carrierCombination52;
	BOOLEAN_t	 carrierCombination26;
	BOOLEAN_t	 carrierCombination62;
	BOOLEAN_t	 carrierCombination33;
	BOOLEAN_t	 carrierCombination34;
	BOOLEAN_t	 carrierCombination43;
	BOOLEAN_t	 carrierCombination44;
	BOOLEAN_t	 carrierCombination35;
	BOOLEAN_t	 carrierCombination53;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} SupportedCarrierCombination_ext_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_SupportedCarrierCombination_ext;

#ifdef __cplusplus
}
#endif

#endif	/* _SupportedCarrierCombination_ext_H_ */
#include <asn_internal.h>