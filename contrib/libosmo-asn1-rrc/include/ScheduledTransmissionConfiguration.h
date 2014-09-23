/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#ifndef	_ScheduledTransmissionConfiguration_H_
#define	_ScheduledTransmissionConfiguration_H_


#include <asn_application.h>

/* Including external dependencies */
#include <asn_SEQUENCE_OF.h>
#include <constr_SEQUENCE_OF.h>
#include <constr_SEQUENCE.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Forward declarations */
struct Common_E_DCH_ResourceInfoListExt;

/* ScheduledTransmissionConfiguration */
typedef struct ScheduledTransmissionConfiguration {
	struct ScheduledTransmissionConfiguration__common_E_DCH_ResourceInfoListExt {
		A_SEQUENCE_OF(struct Common_E_DCH_ResourceInfoListExt) list;
		
		/* Context for parsing across buffer boundaries */
		asn_struct_ctx_t _asn_ctx;
	} common_E_DCH_ResourceInfoListExt;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} ScheduledTransmissionConfiguration_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_ScheduledTransmissionConfiguration;

#ifdef __cplusplus
}
#endif

/* Referred external types */
#include "Common-E-DCH-ResourceInfoListExt.h"

#endif	/* _ScheduledTransmissionConfiguration_H_ */
#include <asn_internal.h>