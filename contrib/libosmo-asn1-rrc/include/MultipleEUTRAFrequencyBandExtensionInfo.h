/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#ifndef	_MultipleEUTRAFrequencyBandExtensionInfo_H_
#define	_MultipleEUTRAFrequencyBandExtensionInfo_H_


#include <asn_application.h>

/* Including external dependencies */
#include <constr_SEQUENCE.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Forward declarations */
struct MultipleEUTRAFrequencyBandIndicatorExtensionList;

/* MultipleEUTRAFrequencyBandExtensionInfo */
typedef struct MultipleEUTRAFrequencyBandExtensionInfo {
	struct MultipleEUTRAFrequencyBandIndicatorExtensionList	*multipleEUTRAFrequencyBandIndicatorlist	/* OPTIONAL */;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} MultipleEUTRAFrequencyBandExtensionInfo_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_MultipleEUTRAFrequencyBandExtensionInfo;

#ifdef __cplusplus
}
#endif

/* Referred external types */
#include "MultipleEUTRAFrequencyBandIndicatorExtensionList.h"

#endif	/* _MultipleEUTRAFrequencyBandExtensionInfo_H_ */
#include <asn_internal.h>