/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#include "UplinkTimeslotsCodes.h"

static int
memb_numAdditionalTimeslots_constraint_8(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 1 && value <= 13)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static int
memb_timeslotList_constraint_7(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	/* Determine the number of elements */
	size = _A_CSEQUENCE_FROM_VOID(sptr)->count;
	
	if((size >= 1 && size <= 13)) {
		/* Perform validation of the inner elements */
		return td->check_constraints(td, sptr, ctfailcb, app_key);
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_numAdditionalTimeslots_constr_9 = {
	{ APC_CONSTRAINED,	 4,  4,  1,  13 }	/* (1..13) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	0, 0	/* No PER value map */
};
static asn_per_constraints_t asn_PER_type_timeslotList_constr_10 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 4,  4,  1,  13 }	/* (SIZE(1..13)) */,
	0, 0	/* No PER value map */
};
static asn_per_constraints_t asn_PER_memb_timeslotList_constr_10 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 4,  4,  1,  13 }	/* (SIZE(1..13)) */,
	0, 0	/* No PER value map */
};
static asn_per_constraints_t asn_PER_type_additionalTimeslots_constr_7 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	0, 0	/* No PER value map */
};
static asn_per_constraints_t asn_PER_type_moreTimeslots_constr_5 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	0, 0	/* No PER value map */
};
static asn_TYPE_member_t asn_MBR_consecutive_8[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots__consecutive, numAdditionalTimeslots),
		(ASN_TAG_CLASS_CONTEXT | (0 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_NativeInteger,
		memb_numAdditionalTimeslots_constraint_8,
		&asn_PER_memb_numAdditionalTimeslots_constr_9,
		0,
		"numAdditionalTimeslots"
		},
};
static ber_tlv_tag_t asn_DEF_consecutive_tags_8[] = {
	(ASN_TAG_CLASS_CONTEXT | (0 << 2)),
	(ASN_TAG_CLASS_UNIVERSAL | (16 << 2))
};
static asn_TYPE_tag2member_t asn_MAP_consecutive_tag2el_8[] = {
    { (ASN_TAG_CLASS_CONTEXT | (0 << 2)), 0, 0, 0 } /* numAdditionalTimeslots at 12812 */
};
static asn_SEQUENCE_specifics_t asn_SPC_consecutive_specs_8 = {
	sizeof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots__consecutive),
	offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots__consecutive, _asn_ctx),
	asn_MAP_consecutive_tag2el_8,
	1,	/* Count of tags in the map */
	0, 0, 0,	/* Optional elements (not needed) */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_consecutive_8 = {
	"consecutive",
	"consecutive",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_ber,
	SEQUENCE_encode_der,
	SEQUENCE_decode_xer,
	SEQUENCE_encode_xer,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* Use generic outmost tag fetcher */
	asn_DEF_consecutive_tags_8,
	sizeof(asn_DEF_consecutive_tags_8)
		/sizeof(asn_DEF_consecutive_tags_8[0]) - 1, /* 1 */
	asn_DEF_consecutive_tags_8,	/* Same as above */
	sizeof(asn_DEF_consecutive_tags_8)
		/sizeof(asn_DEF_consecutive_tags_8[0]), /* 2 */
	0,	/* No PER visible constraints */
	asn_MBR_consecutive_8,
	1,	/* Elements count */
	&asn_SPC_consecutive_specs_8	/* Additional specs */
};

static asn_TYPE_member_t asn_MBR_timeslotList_10[] = {
	{ ATF_POINTER, 0, 0,
		(ASN_TAG_CLASS_UNIVERSAL | (16 << 2)),
		0,
		&asn_DEF_UplinkAdditionalTimeslots,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		""
		},
};
static ber_tlv_tag_t asn_DEF_timeslotList_tags_10[] = {
	(ASN_TAG_CLASS_CONTEXT | (1 << 2)),
	(ASN_TAG_CLASS_UNIVERSAL | (16 << 2))
};
static asn_SET_OF_specifics_t asn_SPC_timeslotList_specs_10 = {
	sizeof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots__timeslotList),
	offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots__timeslotList, _asn_ctx),
	0,	/* XER encoding is XMLDelimitedItemList */
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_timeslotList_10 = {
	"timeslotList",
	"timeslotList",
	SEQUENCE_OF_free,
	SEQUENCE_OF_print,
	SEQUENCE_OF_constraint,
	SEQUENCE_OF_decode_ber,
	SEQUENCE_OF_encode_der,
	SEQUENCE_OF_decode_xer,
	SEQUENCE_OF_encode_xer,
	SEQUENCE_OF_decode_uper,
	SEQUENCE_OF_encode_uper,
	0,	/* Use generic outmost tag fetcher */
	asn_DEF_timeslotList_tags_10,
	sizeof(asn_DEF_timeslotList_tags_10)
		/sizeof(asn_DEF_timeslotList_tags_10[0]) - 1, /* 1 */
	asn_DEF_timeslotList_tags_10,	/* Same as above */
	sizeof(asn_DEF_timeslotList_tags_10)
		/sizeof(asn_DEF_timeslotList_tags_10[0]), /* 2 */
	&asn_PER_type_timeslotList_constr_10,
	asn_MBR_timeslotList_10,
	1,	/* Single element */
	&asn_SPC_timeslotList_specs_10	/* Additional specs */
};

static asn_TYPE_member_t asn_MBR_additionalTimeslots_7[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots, choice.consecutive),
		(ASN_TAG_CLASS_CONTEXT | (0 << 2)),
		0,
		&asn_DEF_consecutive_8,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"consecutive"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots, choice.timeslotList),
		(ASN_TAG_CLASS_CONTEXT | (1 << 2)),
		0,
		&asn_DEF_timeslotList_10,
		memb_timeslotList_constraint_7,
		&asn_PER_memb_timeslotList_constr_10,
		0,
		"timeslotList"
		},
};
static asn_TYPE_tag2member_t asn_MAP_additionalTimeslots_tag2el_7[] = {
    { (ASN_TAG_CLASS_CONTEXT | (0 << 2)), 0, 0, 0 }, /* consecutive at 12813 */
    { (ASN_TAG_CLASS_CONTEXT | (1 << 2)), 1, 0, 0 } /* timeslotList at 12816 */
};
static asn_CHOICE_specifics_t asn_SPC_additionalTimeslots_specs_7 = {
	sizeof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots),
	offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots, _asn_ctx),
	offsetof(struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots, present),
	sizeof(((struct UplinkTimeslotsCodes__moreTimeslots__additionalTimeslots *)0)->present),
	asn_MAP_additionalTimeslots_tag2el_7,
	2,	/* Count of tags in the map */
	0,
	-1	/* Extensions start */
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_additionalTimeslots_7 = {
	"additionalTimeslots",
	"additionalTimeslots",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_ber,
	CHOICE_encode_der,
	CHOICE_decode_xer,
	CHOICE_encode_xer,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	CHOICE_outmost_tag,
	0,	/* No effective tags (pointer) */
	0,	/* No effective tags (count) */
	0,	/* No tags (pointer) */
	0,	/* No tags (count) */
	&asn_PER_type_additionalTimeslots_constr_7,
	asn_MBR_additionalTimeslots_7,
	2,	/* Elements count */
	&asn_SPC_additionalTimeslots_specs_7	/* Additional specs */
};

static asn_TYPE_member_t asn_MBR_moreTimeslots_5[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes__moreTimeslots, choice.noMore),
		(ASN_TAG_CLASS_CONTEXT | (0 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_NULL,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"noMore"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes__moreTimeslots, choice.additionalTimeslots),
		(ASN_TAG_CLASS_CONTEXT | (1 << 2)),
		+1,	/* EXPLICIT tag at current level */
		&asn_DEF_additionalTimeslots_7,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"additionalTimeslots"
		},
};
static asn_TYPE_tag2member_t asn_MAP_moreTimeslots_tag2el_5[] = {
    { (ASN_TAG_CLASS_CONTEXT | (0 << 2)), 0, 0, 0 }, /* noMore at 12809 */
    { (ASN_TAG_CLASS_CONTEXT | (1 << 2)), 1, 0, 0 } /* additionalTimeslots at 12813 */
};
static asn_CHOICE_specifics_t asn_SPC_moreTimeslots_specs_5 = {
	sizeof(struct UplinkTimeslotsCodes__moreTimeslots),
	offsetof(struct UplinkTimeslotsCodes__moreTimeslots, _asn_ctx),
	offsetof(struct UplinkTimeslotsCodes__moreTimeslots, present),
	sizeof(((struct UplinkTimeslotsCodes__moreTimeslots *)0)->present),
	asn_MAP_moreTimeslots_tag2el_5,
	2,	/* Count of tags in the map */
	0,
	-1	/* Extensions start */
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_moreTimeslots_5 = {
	"moreTimeslots",
	"moreTimeslots",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_ber,
	CHOICE_encode_der,
	CHOICE_decode_xer,
	CHOICE_encode_xer,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	CHOICE_outmost_tag,
	0,	/* No effective tags (pointer) */
	0,	/* No effective tags (count) */
	0,	/* No tags (pointer) */
	0,	/* No tags (count) */
	&asn_PER_type_moreTimeslots_constr_5,
	asn_MBR_moreTimeslots_5,
	2,	/* Elements count */
	&asn_SPC_moreTimeslots_specs_5	/* Additional specs */
};

static asn_TYPE_member_t asn_MBR_UplinkTimeslotsCodes_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes, dynamicSFusage),
		(ASN_TAG_CLASS_CONTEXT | (0 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"dynamicSFusage"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes, firstIndividualTimeslotInfo),
		(ASN_TAG_CLASS_CONTEXT | (1 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_IndividualTimeslotInfo,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"firstIndividualTimeslotInfo"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes, ul_TS_ChannelisationCodeList),
		(ASN_TAG_CLASS_CONTEXT | (2 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_UL_TS_ChannelisationCodeList,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"ul-TS-ChannelisationCodeList"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UplinkTimeslotsCodes, moreTimeslots),
		(ASN_TAG_CLASS_CONTEXT | (3 << 2)),
		+1,	/* EXPLICIT tag at current level */
		&asn_DEF_moreTimeslots_5,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"moreTimeslots"
		},
};
static ber_tlv_tag_t asn_DEF_UplinkTimeslotsCodes_tags_1[] = {
	(ASN_TAG_CLASS_UNIVERSAL | (16 << 2))
};
static asn_TYPE_tag2member_t asn_MAP_UplinkTimeslotsCodes_tag2el_1[] = {
    { (ASN_TAG_CLASS_CONTEXT | (0 << 2)), 0, 0, 0 }, /* dynamicSFusage at 12805 */
    { (ASN_TAG_CLASS_CONTEXT | (1 << 2)), 1, 0, 0 }, /* firstIndividualTimeslotInfo at 12806 */
    { (ASN_TAG_CLASS_CONTEXT | (2 << 2)), 2, 0, 0 }, /* ul-TS-ChannelisationCodeList at 12807 */
    { (ASN_TAG_CLASS_CONTEXT | (3 << 2)), 3, 0, 0 } /* moreTimeslots at 12809 */
};
static asn_SEQUENCE_specifics_t asn_SPC_UplinkTimeslotsCodes_specs_1 = {
	sizeof(struct UplinkTimeslotsCodes),
	offsetof(struct UplinkTimeslotsCodes, _asn_ctx),
	asn_MAP_UplinkTimeslotsCodes_tag2el_1,
	4,	/* Count of tags in the map */
	0, 0, 0,	/* Optional elements (not needed) */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_UplinkTimeslotsCodes = {
	"UplinkTimeslotsCodes",
	"UplinkTimeslotsCodes",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_ber,
	SEQUENCE_encode_der,
	SEQUENCE_decode_xer,
	SEQUENCE_encode_xer,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* Use generic outmost tag fetcher */
	asn_DEF_UplinkTimeslotsCodes_tags_1,
	sizeof(asn_DEF_UplinkTimeslotsCodes_tags_1)
		/sizeof(asn_DEF_UplinkTimeslotsCodes_tags_1[0]), /* 1 */
	asn_DEF_UplinkTimeslotsCodes_tags_1,	/* Same as above */
	sizeof(asn_DEF_UplinkTimeslotsCodes_tags_1)
		/sizeof(asn_DEF_UplinkTimeslotsCodes_tags_1[0]), /* 1 */
	0,	/* No PER visible constraints */
	asn_MBR_UplinkTimeslotsCodes_1,
	4,	/* Elements count */
	&asn_SPC_UplinkTimeslotsCodes_specs_1	/* Additional specs */
};
